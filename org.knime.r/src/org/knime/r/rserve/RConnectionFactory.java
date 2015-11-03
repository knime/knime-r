package org.knime.r.rserve;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * RConnectionFactory
 * 
 * Factory for {@link RConnection} and R processes.
 * 
 * @author Jonathan Hale
 */
public class RConnectionFactory {

	private static ArrayList<RInstance> m_instances = new ArrayList<>();

	private static RInstance launchRserve(String cmd, String host, int port) {
		return launchRserve(cmd, "--no-save --slave", host, port);
	}

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
	 * @param rargs
	 *            arguments are are to be passed to R
	 * @param rsrvargs
	 *            arguments to be passed to Rserve
	 * @return <code>true</code> if Rserve is running or was successfully
	 *         started, <code>false</code> otherwise.
	 */
	private static RInstance launchRserve(String cmd, String rargs, String host, Integer port) {
		try {
			Process p;
			String osname = System.getProperty("os.name");
			if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
				ProcessBuilder builder = new ProcessBuilder().command(cmd, "--RS-port", port.toString(), "--RS-host",
						host, rargs);
				builder.environment()
						.put("path",
								org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome()
										+ File.pathSeparator
										+ org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider()
												.getRHome()
										+ "\\bin\\x64\\" // TODO arch switch
										+ File.pathSeparator + System.getenv("path"));
				builder.environment().put("R_HOME",
						org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome());
				builder.directory(new File(cmd.replaceAll("Rserve.exe", "")));
				p = builder.start();
			} else /* unix startup */ {
				// TODO
				throw new RuntimeException("Launching RServe under Unix is not implemented yet.");
			}

			RInstance rInstance = new RInstance(p, host, port);
			m_instances.add(rInstance);

			return rInstance;
		} catch (Exception x) {
			System.out.println("Rserve: failed to start Rserve process with " + x.getMessage());
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
			if (!inst.getLastConnection().isConnected()) {
				return inst.createConnection();
			}
		}

		// find any available port to run RServe on
		int port = 6311;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return launchRserve(org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRServeBinPath(),
				"127.0.0.1", port).createConnection();
	}

	public static void terminateProcessOf(final RConnection connection) {
		// find the RInstance to (forcefully) terminate
		RInstance inst = null;
		final Iterator<RInstance> itor = m_instances.iterator();
		while (itor.hasNext()) {
			inst = itor.next();
			if (inst.getLastConnection() == connection) {
				itor.remove();
				break;
			}
		}

		if (inst == null) {
			return;
		}

		// We cannot shutdown server the nice way, since it blocks if other
		// commands are currently being executed:
		// connection.serverShutdown();
		// Also, connection is expected to be closed.

		// force shutdown server by terminating the process.
		inst.getProcess().destroy();

		if (inst.getProcess().isAlive()) {
			// very persistent process
			inst.getProcess().destroyForcibly();
		}
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

		public RInstance(final Process p, final String host, final int port) {
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

		@Override
		protected void finalize() throws Throwable {
			// WARNING: finalize may or may not be called.
			close();
			super.finalize();
		}

		/**
		 * Potentially forcefully terminate the process.
		 */
		public Process getProcess() {
			return m_process;
		}

	}

}
