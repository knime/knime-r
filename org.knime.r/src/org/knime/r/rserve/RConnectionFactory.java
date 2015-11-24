package org.knime.r.rserve;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.KNIMETimer;
import org.knime.r.controller.RController;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.sun.jna.Platform;

/**
 * RConnectionFactory
 *
 * Factory for {@link RConnection} and R processes.
 *
 * @author Jonathan Hale
 */
public class RConnectionFactory {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(RController.class);

	private static final ArrayList<RConnectionResource> m_resources = new ArrayList<>();
	/*
	 * Whether the shutdown hooks have been added. Using atomic boolean enables
	 * us to make sure we only add the shutdown hooks once.
	 */
	private static final AtomicBoolean m_initialized = new AtomicBoolean(false);

	/**
	 * For testing purposes.
	 *
	 * @return Read only collection of currently running Rserve processes.
	 */
	public static Collection<Process> getRunningRProcesses() {
		final ArrayList<Process> list = new ArrayList<>();

		for (final RConnectionResource res : m_resources) {
			final Process p = res.getUnderlyingRInstance().getProcess();

			if (p.isAlive()) {
				list.add(p);
			}
		}

		return list;
	}

	/**
	 * Attempt to start Rserve and create a connection to it.
	 *
	 * @param cmd
	 *            command necessary to start Rserve ("Rserve.exe" on Windows)
	 * @return <code>true</code> if Rserve is running or was successfully
	 *         started, <code>false</code> otherwise.
	 * @throws IOException
	 *             if Rserve could not be launched. This may be the case if R is
	 *             either not found or does not have Rserve package installed.
	 */
	private static RInstance launchRserve(final String cmd, final String host, final Integer port) throws IOException {

		final String rHome = org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome();

		RInstance rInstance = null;
		try {
			File commandFile = new File(cmd);
			if (!commandFile.exists()) {
				throw new IOException("Command not found: " + cmd);
			}
			if (!commandFile.canExecute()) {
				throw new IOException("Command is not an executable: " + cmd);
			}

			final ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(), "--vanilla");
			if (Platform.isWindows()) {
				// on windows, the Rserve executable is not reside in the R bin
				// folder, but still requires the R.dll, so we need to put the R
				// bin folder on path
				builder.environment().put("path",
						rHome + File.pathSeparator + rHome + ((Platform.is64Bit()) ? "\\bin\\x64\\" : "\\bin\\i386\\")
								+ File.pathSeparator + System.getenv("path"));
			}

			// R HOME is required for Rserve/R to know where default libraries
			// are located.
			builder.environment().put("R_HOME", rHome);
			builder.directory(new File(cmd).getParentFile());

			final Process p = builder.start();

			/*
			 * Consume output of process, to ensure buffer does not fill up,
			 * which blocks processes on some OSs. Also, we can log errors in
			 * the external process this way.
			 */
			new StreamReaderThread(p.getInputStream(), "R Output Reader (port: " + port + ")", (line) -> {
				/* discard */ }).start();
			new StreamReaderThread(p.getErrorStream(), "R Error Reader (port:" + port + ")",
					(line) -> LOGGER.debug(line)).start();

			// wrap the process, requires host and port to create RConnections
			// later.
			rInstance = new RInstance(p, host, port);

			// try connecting up to 5 times over the course of 500ms. Attempts
			// may fail if Rserve is currently starting up.
			for (int i = 1; i <= 5; i++) {
				try {
					RConnection connection = rInstance.createConnection();
					if (connection != null) {
						LOGGER.debug("Connected to Rserve in " + i + " attempts.");
						break;
					}
				} catch (RserveException e) {
					LOGGER.debug("An attempt (" + i + "/5) to connect to Rserve failed.", e);
					Thread.sleep(100);
				}
			}

			if (rInstance.getLastConnection() == null) {
				throw new IOException("Could not connect to RServe (host: " + host + ", port: " + port + ").");
			}

			return rInstance;
		} catch (Exception x) {
			if (rInstance != null) {
				// terminate the R process incase still running
				rInstance.close();
			}
			throw new IOException("Could not start Rserve process.", x);
		} finally {

		}

	}

	/**
	 * Create a new {@link RConnection}, creating a new R instance beforehand,
	 * if on windows, for every single connection, unless a connection of an
	 * existing instance has been closed in which case an R instance
	 * weleratorConfiguration, org.eclipse.ui.contexts.will be reused.
	 *
	 * The method does not check {@link RConnection#isConnected()}.
	 *
	 * @return an RConnectionResource which has already been acquired, never
	 *         <code>null</code>
	 * @throws RserveException
	 * @throws IOException
	 *             if Rserve could not be launched. This may be the case if R is
	 *             either not found or does not have Rserve package installed.
	 *             Or if there was no open port found.
	 */
	public static RConnectionResource createConnection() throws RserveException, IOException {
		initializeShutdownHook(); // checks for re-initialization

		// synchronizing on the entire class would completely lag out KNIME for
		// some reason
		synchronized (m_resources) {
			// try to reuse an existing instance. Ensures there is max one R
			// instance per parallel executed node.
			for (RConnectionResource resource : m_resources) {
				if (resource.acquireIfAvailable()) {
					return resource;
				}
			}
			// no existing resource is available. Create a new one.

			final RInstance instance = launchRserve(
					org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRServeBinPath(),
					"127.0.0.1", findFreePort());
			RConnectionResource resource = new RConnectionResource(instance);
			resource.acquire();
			m_resources.add(resource);
			return resource;
		}
	}

	/*
	 * Find a free port to launch Rserve on
	 */
	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Could not find a free port for Rserve. Is KNIME not permitted to open ports?", e);
		}
	}

	/*
	 * Add the Shutdown hook. Does nothing if already called once.
	 */
	private static void initializeShutdownHook() {
		// if (m_initialized != false) return;
		// else m_initialized = true;
		if (m_initialized.compareAndSet(false, true)) {
			/* already initialized */
			return;
		}

		// m_initialized was false, we need to initialize.

		/*
		 * Cleanup remaining Rserve processes on VM exit.
		 */
		Runtime.getRuntime().addShutdownHook(new Thread("R Processes Cleanup") {

			@Override
			public void run() {
				synchronized (m_resources) {
					for (final RConnectionResource resource : m_resources) {
						if (resource != null && resource.getUnderlyingRInstance() != null) {
							resource.destroy(false);
						}
					}
				}
			}
		});

		// m_initialize already set to true in compareAndSet
	}

	/**
	 * An instance of R running in an external process. The process is
	 * terminated on {@link #close()}.
	 *
	 * @author Jonathan Hale
	 */
	private static class RInstance implements AutoCloseable {
		private final Process m_process;
		private final String m_host;
		private final int m_port;

		private RConnection m_lastConnection = null;

		/**
		 * Constructor
		 *
		 * @param p
		 *            An Rserve process
		 * @param host
		 *            Host on which the Rserve process is running. TODO:
		 *            Currently always localhost.
		 * @param port
		 *            Port on which Rserve is running.
		 */
		private RInstance(final Process p, final String host, final int port) {
			m_process = p;
			m_host = host;
			m_port = port;
		}

		public RConnection createConnection() throws RserveException {
			m_lastConnection = new RConnection(m_host, m_port);
			return m_lastConnection;
		}

		public RConnection getLastConnection() {
			return m_lastConnection;
		}

		@Override
		public void close() {
			// terminate processes the nicer way
			m_process.destroy();

			// make sure the processes really are terminated
			if (m_process.isAlive()) {
				m_process.destroyForcibly();
			}
		}

		/**
		 * Potentially forcefully terminate the process.
		 */
		public Process getProcess() {
			return m_process;
		}

		/**
		 * @return Whether this Instance is up and running.
		 */
		public boolean isAlive() {
			return m_process != null && m_process.isAlive();
		}

	}

	/**
	 * Thread which processes an InputStream line by line via a processing
	 * function specified by the user.
	 *
	 * @author Jonathan Hale
	 */
	private static final class StreamReaderThread extends Thread {

		/**
		 * Interface for functions processing input line by line.
		 *
		 * @author Jonathan Hale
		 */
		@FunctionalInterface
		interface LineProcessor {
			void processLine(String s);
		}

		private final InputStream m_stream;
		private final LineProcessor m_processor;

		/**
		 * Constructor
		 *
		 * @param stream
		 *            to read from
		 * @param name
		 *            for the Thread
		 * @param processor
		 *            to process lines
		 */
		public StreamReaderThread(final InputStream stream, final String name, final LineProcessor processor) {
			super(name);
			m_stream = stream;
			m_processor = processor;
		}

		@Override
		public void run() {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(m_stream));
			try {
				String line;
				while ((line = reader.readLine()) != null && !isInterrupted()) {
					m_processor.processLine(line);
				}
			} catch (IOException e) {
				// nothing to do
			}
		}
	}

	/**
	 * This class holds an RInstance and returns its RConnection on
	 * {@link RConnectionResource#get()}. If released, a timeout will make sure
	 * that the underlying {@link RInstance} is shutdown after a certain amount
	 * of time.
	 *
	 * @author Jonathan Hale
	 */
	public static class RConnectionResource {

		private boolean m_available = true;
		private RInstance m_instance;
		private TimerTask m_pendingDestructionTask = null;

		private static final int RPROCESS_TIMEOUT = 60000;

		/**
		 * Constructor
		 *
		 * @param inst
		 *            RInstance which will provide the value of this resource.
		 */
		public RConnectionResource(final RInstance inst) {
			if (inst == null) {
				throw new NullPointerException("The RInstance provided to an RConnectionResource may not be null.");
			}

			m_instance = inst;
		}

		/**
		 * Acquire ownership of this resource. Only the factory should be able
		 * to do this.
		 */
		/* package-protected */ synchronized void acquire() {
			if (m_instance == null) {
				throw new NullPointerException("The resource has been destroyed already.");
			}

			if (m_available) {
				doAcquire();
			} else {
				throw new IllegalStateException("Resource cannot be aquired, it is owned already.");
			}
		}

		/**
		 * Acquire ownership of this resource, if it is available. Only the
		 * factory should be able to do this.
		 *
		 * @return Whether the resource has been acquired.
		 */
		/* package-protected */ synchronized boolean acquireIfAvailable() {
			if (m_instance == null) {
				throw new NullPointerException("The resource has been destroyed already.");
			}

			if (m_available) {
				doAcquire();
				return true;
			} else {
				return false;
			}
		}

		/*
		 * Do everything necessary for acquiring this resource.
		 */
		private void doAcquire() {
			m_available = false;

			if (m_pendingDestructionTask != null) {
				m_pendingDestructionTask.cancel();
				m_pendingDestructionTask = null;
			}
		}

		/**
		 * @return The RInstance which holds this resources RConnection.
		 */
		/* package-protected */ RInstance getUnderlyingRInstance() {
			return m_instance;
		}

		/**
		 * @return <code>true</code> if the resource has not been aquired yet.
		 */
		public synchronized boolean isAvailable() {
			return m_available;
		}

		/**
		 * Note: Always call {@link #acquire()} or {@link #acquireIfAvailable()}
		 * before using this method.
		 *
		 * @return The value of the resource.
		 * @throws IllegalAccessError
		 *             If the resource has not been acquired yet.
		 */
		public RConnection get() {
			if (m_available) {
				throw new IllegalAccessError("Please RConnectionResource#aquire() first before calling get.");
			}
			if (m_instance == null) {
				throw new NullPointerException("The resource has been closed already.");
			}

			return m_instance.getLastConnection();
		}

		/**
		 * Release ownership of this resource for it to be reacquired.
		 */
		public synchronized void release() {
			if (!m_available) {
				// this should never happen, since either
				// m_pendingDestructionTask is null, which means this resource
				// is being held, or the resource is available and has
				// destruction pending.
				assert m_pendingDestructionTask == null;

				m_available = true;

				m_pendingDestructionTask = new TimerTask() {
					@Override
					public synchronized void run() {
						try {
							synchronized (RConnectionResource.this) {
								if (m_available) {
									// if not acquired in the meantime, destroy
									// the resource
									destroy(true);
								}
							}
						} catch (Throwable t) {
							// FIXME: There is a known bug where TimerTasks in
							// KnimeTimer can crash KNIME. We are simply making
							// 100% sure this will not happen here by catching
							// everything.
						}
					}

				};
				KNIMETimer.getInstance().schedule(m_pendingDestructionTask, RPROCESS_TIMEOUT);
			} // else: release had been called already, but we allow this.
		}

		/**
		 * Destroy the underlying resource.
		 *
		 * @param remove
		 *            Whether to automatically remove this resource from
		 *            m_resources.
		 */
		public synchronized void destroy(final boolean remove) {
			if (m_instance == null) {
				throw new NullPointerException("The resource has been destroyed already.");
			}

			m_available = false;
			m_instance.close();

			if (remove) {
				synchronized (m_resources) {
					m_resources.remove(this);
				}
			}
			m_instance = null;

			// cleanup TimerTask
			if (m_pendingDestructionTask != null) {
				m_pendingDestructionTask.cancel();
				m_pendingDestructionTask = null;
			}
		}

		/**
		 * @return whether the underlying RInstance is up and running.
		 */
		public boolean isRInstanceAlive() {
			return m_instance != null && m_instance.isAlive();
		}

	}

}
