package org.knime.r.rserve;

import java.io.File;
import java.io.IOException;
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
			for (RInstance inst : m_instances) {
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

		for (RInstance inst : m_instances) {
			final Process p = inst.getProcess();

			if (p.isAlive()) {
				list.add(p);
			}
		}

		return list;
	}

	/**
	 * attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid
	 * using any quotes in arguments
	 *
	 * @param cmd
	 *            command necessary to start Rserve ("Rserve.exe")
	 * @return <code>true</code> if Rserve is running or was successfully
	 *         started, <code>false</code> otherwise.
	 */
	private static RInstance launchRserve(final String cmd, final String host, final Integer port) {
		try {
			Process p;

			final String rargs = "--no-save --slave";
			if (Platform.isWindows()) {
				final ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(),
						"--RS-host", host, rargs);
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
				builder.directory(new File(cmd.replaceAll("Rserve.exe", "")));
				p = builder.start();
			} else /* unix startup */ {
				final ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(),
						"--RS-host", host, rargs);
				builder.environment().put("R_HOME",
						org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome());
				builder.directory(new File(cmd.replaceAll("Rserve", "")));
				p = builder.start();
			}

			final RInstance rInstance = RInstance.createRInstance(p, host, port); // also
																					// creates
																					// first
																					// connection
			m_instances.add(rInstance);

			return rInstance;
		} catch (Exception x) {
			LOGGER.error(
					"Could not start Rserve process. This may be caused by Rserve package not installed or an invalid or broken R Home.");
			return null;
		}
	}

	/**
	 * Create a new {@link RConnection}, creating a new R instance beforehand,
	 * if on windows, for every single connection, unless a connection of an
	 * existing instance has been closed in which case an R instance will be
	 * reused.
	 * 
	 * The method does not check {@link RConnection#isConnected()}.
	 * 
	 * @throws RserveException
	 */
	public static synchronized RConnection createConnection() throws RserveException {
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
			LOGGER.error("Could not find a free port for Rserve. Is KNIME not permitted to open ports?");
			return null;
		}
		return launchRserve(org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRServeBinPath(),
				"127.0.0.1", port).getLastConnection(); // connection was
														// already created for
														// us.
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
	private static RInstance getRInstOfConnection(RConnection connection, boolean remove) {
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

		public static RInstance createRInstance(final Process p, final String host, final int port)
				throws RserveException {
			final RInstance inst = new RInstance(p, host, port);
			inst.createConnection();
			return inst;
		}

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

}
