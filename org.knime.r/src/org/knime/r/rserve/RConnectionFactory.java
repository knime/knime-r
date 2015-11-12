package org.knime.r.rserve;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.knime.core.node.NodeLogger;
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

	private static ArrayList<RConnectionResource> m_resources = new ArrayList<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (final RConnectionResource resource : m_resources) {
				resource.release();
			}
		}));
	}

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
		try {
			Process p;

			final String rargs = "--vanilla";
			if (Platform.isWindows()) {
				final ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(), rargs);
				builder.environment()
						.put("path",
								org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome()
										+ File.pathSeparator
										+ org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider()
												.getRHome()
										+ ((Platform.is64Bit()) ? "\\bin\\x64\\" : "\\bin\\i386\\") + File.pathSeparator
										+ System.getenv("path"));
				builder.environment().put("R_HOME",
						org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome());
				builder.directory(new File(cmd).getParentFile());
				p = builder.start();
			} else /* unix startup */ {
				final ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(), rargs);
				builder.environment().put("R_HOME",
						org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome());
				builder.directory(new File(cmd).getParentFile());
				p = builder.start();
			}

			new StreamReaderThread(p.getInputStream(), "R Output Reader (port: " + port + ")") {

				@Override
				public void processLine(final String line) {
					// discard
				}
			}.start();

			new StreamReaderThread(p.getErrorStream(), "R Error Reader (port: " + port + ")") {

				@Override
				public void processLine(final String line) {
					LOGGER.debug(line);
				}
			}.start();

			final RInstance rInstance = new RInstance(p, host, port);

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
					Thread.sleep(100);
				}
			}

			if (rInstance.getLastConnection() == null) {
				throw new IOException("Could not connect to RServe.");
			}

			return rInstance;
		} catch (Exception x) {
			throw new IOException(
					"Could not start Rserve process. This may be caused by Rserve package not installed or an invalid or broken R Home.",
					x);
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
	public static synchronized RConnectionResource createConnection() throws RserveException, IOException {
		// try to reuse an existing instance. Ensures there is max one R
		// instance per parallel executed node.
		for (RConnectionResource resource : m_resources) {
			if (resource.acquireIfAvailable()) {
				return resource;
			}
		}
		// no existing resource is available. Create a new one.

		// find any available port to run RServe on
		int port = 6311;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Could not find a free port for Rserve. Is KNIME not permitted to open ports?", e);
		}
		final RInstance instance = launchRserve(
				org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRServeBinPath(), "127.0.0.1",
				port);
		RConnectionResource resource = new RConnectionResource(instance);
		resource.acquire();
		m_resources.add(resource);
		return resource;
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

	private static abstract class StreamReaderThread extends Thread {

		private final InputStream m_stream;

		/**
		 *
		 */
		public StreamReaderThread(final InputStream stream, final String name) {
			super(name);
			m_stream = stream;
		}

		@Override
		public void run() {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(m_stream));
			try {
				String line;
				while ((line = reader.readLine()) != null && !isInterrupted()) {
					processLine(line);
				}
			} catch (IOException e) {
				// nothing to do
			}
		}

		public abstract void processLine(final String line);
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
				m_available = false;
			} else {
				throw new IllegalStateException("Resource can not be aquired, it is owned already.");
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
				m_available = false;
				return true;
			} else {
				return false;
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
		 * Release ownership of this resource for it to be reaquired.
		 */
		public synchronized void release() {
			if (!m_available) {
				m_available = true;

				final TimerTask task = new TimerTask() {
					@Override
					public void run() {
						if (m_available) {
							// if not acquired in the meantime, destroy the
							// resource
							destroy();
						}
					}

				};
				new Timer().schedule(task, RPROCESS_TIMEOUT);
			} // else: release had been called already, but we allow this.
		}

		public synchronized void destroy() {
			synchronized (m_resources) {
				if (m_instance == null) {
					throw new NullPointerException("The resource has been destroyed already.");
				}

				m_available = false;
				m_instance.close();
				m_resources.remove(this);
				m_instance = null;
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