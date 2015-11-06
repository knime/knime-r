package org.knime.r.rserve;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.knime.core.node.NodeLogger;
import org.knime.r.RController;
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

	private static ArrayList<RInstance> m_instances = new ArrayList<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (final RInstance inst : m_instances) {
				inst.close();
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

		for (final RInstance inst : m_instances) {
			final Process p = inst.getProcess();

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

				new StreamReaderThread(p.getInputStream(), "R Output Reader") {

					@Override
					public void processLine(final String line) {
						// discard
					}
				}.start();

				new StreamReaderThread(p.getErrorStream(), "R Error Reader") {

					@Override
					public void processLine(final String line) {
						LOGGER.debug(line);
					}
				}.start();

			}
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

			m_instances.add(rInstance);

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
	 * @return an RConnection, never <code>null</code>
	 * @throws RserveException
	 * @throws IOException
	 *             if Rserve could not be launched. This may be the case if R is
	 *             either not found or does not have Rserve package installed.
	 *             Or if there was no open port found.
	 */
	public static synchronized RConnection createConnection() throws RserveException, IOException {
		// try to reuse an existing instance. Ensures there is max one R
		// instance per parallel executed node.
		for (RInstance inst : m_instances) {
			if (!inst.getLastConnection().isConnected()) { // getLastConnection()
															// is never null
				return inst.createConnection();
			}
		}

		// find any available port to run RServe on
		int port = 6311;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Could not find a free port for Rserve. Is KNIME not permitted to open ports?", e);
		}
		return launchRserve(org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRServeBinPath(),
				"127.0.0.1", port).getLastConnection();
	}

	public static void terminateProcessOf(final RConnection connection) {
		// find the RInstance to (forcefully) terminate
		RInstance inst = getRInstOfConnection(connection, true);

		if (inst == null) {
			return;
		}

		// We cannot shutdown server the nice way, since it blocks if other
		// commands are currently being executed:
		// connection.serverShutdown();
		// Also, connection is expected to be closed.

		inst.close();
	}

	/**
	 * Get the {@link RInstance} belonging to the given {@link RConnection}.
	 *
	 * @param connection
	 * @return the R instance
	 */
	private static RInstance getRInstOfConnection(final RConnection connection, final boolean remove) {
		final Iterator<RInstance> itor = m_instances.iterator();
		RInstance inst = null;
		while (itor.hasNext()) {
			inst = itor.next();
			if (inst.getLastConnection() == connection) {
				if (remove) {
					itor.remove();
				}
				return inst;
			}
		}

		return null;
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

	}

	/**
	 * @param connection
	 * @return <code>true</code> if the R instance associated with the given
	 *         connection is up and running.
	 */
	public static boolean isProcessesOfConnectionAlive(final RConnection connection) {
		RInstance inst = getRInstOfConnection(connection, false);
		return inst != null && inst.getProcess().isAlive();
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

}
