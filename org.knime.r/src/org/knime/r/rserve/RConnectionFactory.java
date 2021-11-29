package org.knime.r.rserve;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KNIMETimer;
import org.knime.ext.r.bin.preferences.DefaultRPreferenceProvider;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
import org.knime.r.controller.IRController.RException;
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

    /**
     * For KNIME's R extension: Timeout for the socket to connect to R. Value is integral and in milliseconds (default
     * 30000).
     */
    private static final String PROPERTY_R_RSERVE_CONNECT_TIMEOUT = "knime.r.rserve.connecttimeout";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RController.class);

    private static final boolean DEBUG_RSERVE = Boolean.getBoolean(KNIMEConstants.PROPERTY_R_RSERVE_DEBUG);

    private static final ReentrantLock RESOURCES_LOCK = new ReentrantLock();

    private static final Map<RPreferenceProvider, List<RConnectionResource>> RESOURCES = new HashMap<>();

    /**
     * Class which allows locking a lock in a try-with-resource statement to be implicitly unlocked in finally.
     *
     * <pre>
     * <code>
     *    try(LockHolder lh = new LockHolder(myLock)) {
     *    	// lock() has been called on myLock
     *    }
     *    // unlock() has been called on myLock
     * <code>
     * </pre>
     */
    private static class LockHolder implements AutoCloseable {

        private final Lock m_lock;

        /**
         * Constructor
         *
         * @param lock Lock to lock and unlock
         */
        public LockHolder(final Lock lock) {
            m_lock = lock;
            lock.lock();
        }

        @Override
        public void close() {
            m_lock.unlock();
        }
    }

    private static final File tempDir;

    static {
        // create temporary directory for R
        File dir;
        try {
            // try creating a subdirectory in the KNIME temp folder to have all
            // R stuff in one place.
            dir = FileUtil.createTempDir("knime-r-tmp", KNIMEConstants.getKNIMETempPath().toFile());
        } catch (final IOException e) {
            // this should never happen, but if it does, using the existing
            // KNIME temp folder directly should work well enough.
            LOGGER.warn("Could not create temporary directory for R integration.", e);
            dir = KNIMEConstants.getKNIMETempPath().toFile();
        }

        tempDir = dir;
    }

    /*
     * Whether the shutdown hooks have been added. Using atomic boolean enables
     * us to make sure we only add the shutdown hooks once.
     */
    private static final AtomicBoolean m_initialized = new AtomicBoolean(false);

    /**
     * For testing purposes. All running R processes.
     *
     * @return Read only collection of currently running Rserve processes.
     */
    public static Collection<Process> getRunningRProcesses() {
        final ArrayList<Process> list = new ArrayList<>();

        try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
            for (final RConnectionResource res : getAllResources()) {
                final Process p = res.getUnderlyingRInstance().getProcess();

                if (p.isAlive()) {
                    list.add(p);
                }
            }
        }

        return list;
    }

    /** Get all resources for all preferences. Lock the resources beforehand. */
    private static List<RConnectionResource> getAllResources() {
        return RESOURCES.values().stream() //
            .flatMap(List<RConnectionResource>::stream) //
            .collect(Collectors.toList());
    }

    /**
     * @return Configuration file for Rserve
     */
    private static File createRserveConfig() {
        final File file = new File(tempDir, "Rserve.conf");
        try (FileWriter writer = new FileWriter(file)) {
            // convert preference from MB (more intuitive) to kB (required by Rserve)
            final int bufferSizeInKB = RPreferenceInitializer.getRProvider().getMaxInfBuf() * 1024;
            writer.write("maxinbuf " + bufferSizeInKB + "\n");
            writer.write("maxsendbuf " + bufferSizeInKB + "\n");
            writer.write("encoding utf8\n"); // encoding for java clients

            /* YES, EA! See https://github.com/s-u/Rserve/blob/
             * 4800e9dc1c67cf4fbc14c502dc7615b644610152/src/Rserv.c#L1134
             */
            writer.write("deamon disable\n");
            // keeping this incase the typo one is removed in a future version:
            writer.write("daemon disable\n");
            if (Boolean.getBoolean("java.awt.headless")) {
                // make sure to run R in non-interactive mode when running
                // headless KNIME (see AP-5748)
                writer.write("interactive no\n");
            }
        } catch (final IOException e) {
            LOGGER.warn("Could not write configuration file for Rserve.", e);
        }

        return file;
    }

    /**
     * Start an Rserve process with a given Rserve executable command.
     *
     * @param preferences R preferences
     * @param host Host of the Rserve server
     * @param port Port to start the Rserve server on
     * @return the started Rserve process
     * @throws IOException
     */
    private static Process launchRserveProcess(final RPreferenceProvider preferences, final String host, final Integer port)
        throws IOException {
        // if debugging, launch debug version of Rserve.
        final String command = preferences.getRServeBinPath();
        final String cmd =
            (DEBUG_RSERVE) ? ((Platform.isWindows()) ? command.replace(".exe", "_d.exe") : command + ".dbg") : command;

        final File commandFile = new File(cmd);
        if (!commandFile.exists()) {
            throw new IOException("Command not found: " + cmd);
        }
        if (!commandFile.canExecute()) {
            throw new IOException("Command is not an executable: " + cmd);
        }
        final String rHome = preferences.getRHome();

        final File configFile = createRserveConfig();
        final ProcessBuilder builder = new ProcessBuilder();
        builder.command(cmd, "--RS-port", port.toString(), "--RS-conf", configFile.getAbsolutePath(), "--vanilla");

        final Map<String, String> env = builder.environment();
        if (Platform.isWindows()) {
            final String path = DefaultRPreferenceProvider.findPathVariableName(env);
            // on windows, the Rserve executable is not reside in the R bin
            // folder, but still requires the R.dll, so we need to put the R
            // bin folder on path
            env.put(path, rHome + File.pathSeparator + rHome
                + ((Platform.is64Bit()) ? "\\bin\\x64\\" : "\\bin\\i386\\") + File.pathSeparator + env.get(path));
        } else {
            // on Unix we need priorize the "R_HOME/lib" folder in the
            // LD_LIBRARY_PATH to ensure that the shared libraries of the
            // selected R installation are used.
            env.put("LD_LIBRARY_PATH",
                rHome + File.separator + "lib" + File.pathSeparator + env.get("LD_LIBRARY_PATH"));

        }
        preferences.setUpEnvironment(env);
        // R HOME is required for Rserve/R to know where default libraries
        // are located.
        env.put("R_HOME", rHome);

        // so that we can clean up everything Rserve spits out
        env.put("TMPDIR", tempDir.getAbsolutePath());

        return builder.start();
    }

    /**
     * Attempt to start Rserve and create a connection to it.
     *
     * @param cmd command necessary to start Rserve ("Rserve.exe" on Windows)
     * @param host For creating the RConnection in RInstance. Launching a remote processes is not supported, this should
     *            always be "127.0.0.1"
     * @param port Port to launch the Rserve process on.
     * @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
     * @throws IOException if Rserve could not be launched. This may be the case if R is either not found or does not
     *             have Rserve package installed.
     */
    private static RInstance launchRserve(final RPreferenceProvider preferences, final String host, final Integer port)
        throws IOException {
        RInstance rInstance = null;
        try {
            final Process p = launchRserveProcess(preferences, host, port);

            // wrap the process, requires host and port to create RConnections
            // later.
            rInstance = new RInstance(p, host, port);

            /*
             * Consume output of process, to ensure buffer does not fill up,
             * which blocks processes on some OSs. Also, we can log errors in
             * the external process this way.
             */
            new StreamReaderThread(p.getInputStream(), "R Output Reader (port: " + port + ")", (line) -> {
                if (DEBUG_RSERVE) {
                    LOGGER.debug(line);
                } /* else discard */
            }).start();
            new StreamReaderThread(p.getErrorStream(), "R Error Reader (port:" + port + ")", LOGGER::debug).start();

			// try connecting up to 5 times over the course of `timeout` ms. Attempts
			// may fail if Rserve is currently starting up.
			final Integer timeout = Integer.getInteger(PROPERTY_R_RSERVE_CONNECT_TIMEOUT, 30000);
			int totalTime = 0; // total time waited
			int attempts = 0; // number of attempts

			while (totalTime < timeout) {
				try {
					attempts += 1;
					final RConnection connection = rInstance.createConnection();
					if (connection != null) {
					    LOGGER.debugWithFormat("Connected to Rserve in %d attempt(s) (%dms).", attempts, totalTime);
						break;
					}
				} catch (RserveException e) {
					LOGGER.debug(String.format("Attempt #%d to connect to Rserve failed (waited %dms, timeout %dms)",
                        attempts, totalTime, timeout), e);
					// produces 200, 400, 800, 1600, 3200 until the diff to`timeout` is smaller.
					final int delay = (int)Math.min(Math.pow(2, attempts) * 100, timeout - totalTime);
					Thread.sleep(delay);
					totalTime += delay;
				}
			}
			try {
				if (rInstance.getLastConnection() == null) {
					attempts += 1;
					// try one last time.
					final RConnection connection = rInstance.createConnection();
					if (connection != null) {
						LOGGER.debugWithFormat("Connected to Rserve in %d attempt(s) (%dms).", attempts, totalTime);
					}
				}
			} catch (RserveException e) {
			    LOGGER.debug(String.format("Last attempt to connect to Rserve failed (waited %dms, timeout %dms)",
			        totalTime, timeout), e);
				throw new IOException("Could not connect to RServe (host: " + host + ", port: " + port + ").");
			}

			return rInstance;
		} catch (Exception x) {
			if (rInstance != null) {
				// terminate the R process in case still running
				rInstance.close();
			}
			throw new IOException("Could not start Rserve process.", x);
		}
	}


    /**
     * Create a new {@link RConnection}. A new R instance with default settings is created beforehand if there is no
     * existing R instance with default settings that can be reused.
     *
     * The method does not check {@link RConnection#isConnected()}.
     *
     * @return an RConnectionResource which has already been acquired, never <code>null</code>
     * @throws RserveException
     * @throws IOException if Rserve could not be launched. This may be the case if R is either not found or does not
     *             have Rserve package installed. Or if there was no open port found.
     */
    public static RConnectionResource createConnection() throws RserveException, IOException {
        // Create a connection with the default preferences
        return createConnection(RPreferenceInitializer.getRProvider());
    }

    /**
     * Create a new {@link RConnection}. A new R instance with given settings is created beforehand if there is no
     * existing R instance with the same settings that can be reused.
     *
     * The method does not check {@link RConnection#isConnected()}.
     *
     * @param preferences the R preference for the R instance
     * @return an RConnectionResource which has already been acquired, never <code>null</code>
     * @throws RserveException
     * @throws IOException if Rserve could not be launched. This may be the case if R is either not found or does not
     *             have Rserve package installed. Or if there was no open port found.
     */
    public static RConnectionResource createConnection(final RPreferenceProvider preferences) throws RserveException, IOException {
        initializeShutdownHook(); // checks for re-initialization

        // synchronizing on the entire class would completely lag out KNIME for
        // some reason
        try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
            // The resources with the given preferences
            final List<RConnectionResource> resources = RESOURCES.computeIfAbsent(preferences, p -> new ArrayList<>());
            // try to reuse an existing instance. Ensures there is max one R
            // instance per parallel executed node.
            for (final RConnectionResource resource : resources) {
                if (resource.acquireIfAvailable()) {
                    // connections are closed when released => we need to
                    // reconnect
                    resource.getUnderlyingRInstance().createConnection();

                    return resource;
                }
            }
            // no existing resource is available. Create a new one.

            final RInstance instance =
                launchRserve(preferences, "127.0.0.1", findFreePort());
            final RConnectionResource resource = new RConnectionResource(instance, preferences);
            if (!resource.acquireIfAvailable()) {
                // this could also be an assertion
                throw new IllegalStateException("Newly created RConnectionResource was not available.");
            }
            resources.add(resource);
            return resource;
        }
    }

    /**
     * Set all resources to be destroyed as soon as they become available. This is useful when settings or preferences
     * changed and old Rserve processes (which still use the old settings) should not be used anymore. This is a better
     * alternative to aborting running nodes.
     */
    public static void clearExistingResources() {
        try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
            for (final List<RConnectionResource> resources : RESOURCES.values()) {
                clearResources(resources);
            }
        }
    }

    /**
     * Set all resources with the given preferences to be destroyed as soon as they become available. This is useful
     * when settings or preferences changed and old Rserve processes (which still use the old settings) should not be
     * used anymore. This is a better alternative to aborting running nodes.
     *
     * @param preferences the preferences of the resources which should be cleared
     */
    public static void clearExistingResources(final RPreferenceProvider preferences) {
        try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
            final List<RConnectionResource> resources = RESOURCES.get(preferences);
            if (resources == null) {
                // No resources with preferences: Nothing to do
                return;
            }
            clearResources(resources);
        }
    }

    /** Clear the given resources and remove them from the list of resources. */
    private static void clearResources(final List<RConnectionResource> resources) {
        LOGGER.debugWithFormat("Retiring RServe processes (%d instances)", resources.size());
        final ArrayList<RConnectionResource> destroyedResources = new ArrayList<>();
        for (final RConnectionResource res : resources) {
            res.setDestroyOnAvailable();
            if (res.getUnderlyingRInstance() == null) {
                // resource has been destroyed already and should be removed
                // from list.
                destroyedResources.add(res);
            }
        }
        resources.removeAll(destroyedResources);
    }

    /**
     * Find a free port to launch Rserve on
     *
     * @return a free port number
     * @throws IOException if no port is available
     */
    public static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            throw new IOException("Could not find a free port for Rserve. Is KNIME not permitted to open ports?", e);
        }
    }

    /*
     * Add the Shutdown hook. Does nothing if already called once.
     */
    private static void initializeShutdownHook() {
        if (!m_initialized.compareAndSet(false, true)) {
            /* already initialized */
            return;
        }

        // m_initialized was false (aka. compareAndSet returned true), we need to initialize.

        /*
         * Cleanup remaining Rserve processes on VM exit.
         */
        Runtime.getRuntime().addShutdownHook(new Thread("R Processes Cleanup") {

            @Override
            public void run() {
                try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
                    for (final RConnectionResource resource : getAllResources()) {
                        if ((resource != null) && (resource.getUnderlyingRInstance() != null)) {
                            resource.destroy(false);
                        }
                    }
                }
            }
        });

        // m_initialized already set to true in compareAndSet
    }

    /**
     * An instance of R running in an external process. The process is terminated on {@link #close()}.
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
         * @param p An Rserve process
         * @param host Host on which the Rserve process is running. TODO: Currently always localhost.
         * @param port Port on which Rserve is running.
         */
        private RInstance(final Process p, final String host, final int port) {
            m_process = p;
            m_host = host;
            m_port = port;
        }

        /**
         * @return Connect to Rserve using the host and port given in the constructor.
         * @throws RserveException
         */
        public RConnection createConnection() throws RserveException {
            m_lastConnection = new RConnection(m_host, m_port);
            return m_lastConnection;
        }

        /**
         * @return The RConnection which was connected to last (may be closed).
         */
        public RConnection getLastConnection() {
            return m_lastConnection;
        }

        @Override
        public void close() {

            // close connection to process, if existent
            if ((m_lastConnection != null) && m_lastConnection.isConnected()) {
                m_lastConnection.close();
            }

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
            return (m_process != null) && m_process.isAlive();
        }

    }

    /**
     * Thread which processes an InputStream line by line via a processing function specified by the user.
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
         * @param stream to read from
         * @param name for the Thread
         * @param processor to process lines
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
                while (((line = reader.readLine()) != null) && !isInterrupted()) {
                    m_processor.processLine(line);
                }
            } catch (final IOException e) {
                // nothing to do
            }
        }
    }

    /**
     * This class holds an RInstance and returns its RConnection on {@link RConnectionResource#get()}. If released, a
     * timeout will make sure that the underlying {@link RInstance} is shutdown after a certain amount of time.
     *
     * @author Jonathan Hale
     */
    public static class RConnectionResource {

        private final RPreferenceProvider m_preferences;

        private final AtomicBoolean m_available = new AtomicBoolean(true);

        private boolean m_destroyOnAvailable = false;

        private RInstance m_instance;

        private TimerTask m_pendingDestructionTask = null;

        private static final int RPROCESS_TIMEOUT = 60000;

        /**
         * Constructor
         *
         * @param inst RInstance which will provide the value of this resource.
         * @param preferences the preferences
         */
        public RConnectionResource(final RInstance inst, final RPreferenceProvider preferences) {
            m_preferences = preferences;
            if (inst == null) {
                throw new NullPointerException("The RInstance provided to an RConnectionResource may not be null.");
            }

            m_instance = inst;
        }

        /**
         * Acquire ownership of this resource, if it is available. Only the factory should be able to do this.
         *
         * @return Whether the resource has been acquired.
         */
        synchronized boolean acquireIfAvailable() {
            if (m_instance == null) {
                throw new NullPointerException("The resource has been destroyed already.");
            }

            if (m_available.compareAndSet(true, false)) {
                if (m_pendingDestructionTask != null) {
                    m_pendingDestructionTask.cancel();
                    m_pendingDestructionTask = null;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * @return The RInstance which holds this resources RConnection.
         */
        RInstance getUnderlyingRInstance() {
            return m_instance;
        }

        /**
         * Check whether the resource has been acquired. A return value of <code>true</code> does <em>not</em> guarantee
         * {@link #acquireIfAvailable()} is going to succeed.
         *
         * @return <code>true</code> if the resource has not been acquired yet.
         */
        public boolean isAvailable() {
            return m_available.get();
        }

        /**
         * Set whether to destroy this resource as soon as the resource becomes available (may destroy the resource
         * immediately).
         */
        public void setDestroyOnAvailable() {
            if (m_available.compareAndSet(true, false)) {
                destroy(false);
            } else {
                m_destroyOnAvailable = true;
            }
        }

        /**
         * Note: Always call {@link #acquire()} or {@link #acquireIfAvailable()} before using this method.
         *
         * @return The value of the resource.
         * @throws IllegalAccessError If the resource has not been acquired yet.
         */
        public RConnection get() {
            if (isAvailable()) {
                throw new IllegalAccessError("Please RConnectionResource#acquire() first before calling get.");
            }
            if (m_instance == null) {
                throw new NullPointerException("The resource has been closed already.");
            }

            return m_instance.getLastConnection();
        }

        /**
         * Release ownership of this resource for it to be reacquired.
         *
         * @throws RException If the RConnection could not be closed/detached
         */
        public synchronized void release() throws RException {
            // we allow release() to be called multiple times (though synchronized) but ignore invocations
            // when already released
            if (!m_available.get()) {
                if ((m_instance.getLastConnection() != null) && m_instance.getLastConnection().isConnected()) {
                    // connection was not closed before release. Clean that up.
                    final RConnection connection = m_instance.getLastConnection();
                    try {

                        // m_instance.getLastConnection().detach(); would be the
                        // way to go, but...
                        // FIXME: https://github.com/s-u/REngine/issues/7

                        // clear workspace in the same method used in
                        // RController. This is copied (!) code,
                        // since considered a (hopefully) temporary option until
                        // the above issue is resolved.
                        if (Platform.isWindows()) {
                            final StringBuilder b = new StringBuilder();
                            b.append("unloader <- function() {\n");
                            b.append("  defaults = getOption(\"defaultPackages\")\n");
                            b.append("  installed = (.packages())\n");
                            b.append("  for (pkg in installed){\n");
                            b.append("      if (!(as.character(pkg) %in% defaults)) {\n");
                            b.append("          if(!(pkg == \"base\")){\n");
                            b.append("              package_name = paste(\"package:\", as.character(pkg), sep=\"\")\n");
                            b.append("              detach(package_name, character.only = TRUE)\n");
                            b.append("          }\n");
                            b.append("      }\n");
                            b.append("  }\n");
                            b.append("}\n");
                            b.append("unloader();\n");
                            b.append("rm(list = ls());"); // also includes the
                            // unloader function
                            connection.eval(b.toString());
                        } // unix automatically gets independent workspaces for
                          // every connection
                    } catch (final RserveException e) {
                        throw new RException(
                            "Could not detach connection to R, could leak objects to other workspaces.", e);
                    } finally {
                        connection.close();
                    }
                }

                if (m_destroyOnAvailable) {
                    destroy(true);
                } else {
                    // Either m_pendingDestructionTask is null, which means
                    // this resource is being held, or the resource is available
                    // and has destruction pending.
                    assert m_pendingDestructionTask == null;

                    m_pendingDestructionTask = new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (m_available.compareAndSet(true, false)) {
                                    // if not acquired in the meantime,
                                    // destroy the resource
                                    destroy(true);
                                }
                            } catch (final Throwable t) {
                                // FIXME: There is a known bug where TimerTasks
                                // in KnimeTimer can crash KNIME. We are simply
                                // making 100% sure this will not happen here by
                                // catching everything.
                            }
                        }

                    };
                    KNIMETimer.getInstance().schedule(m_pendingDestructionTask, RPROCESS_TIMEOUT);
                    m_available.set(true);
                }
            } // else: release had been called already, but we allow this.
        }

        /**
         * Destroy the underlying resource.
         *
         * @param remove Whether to automatically remove this resource from m_resources.
         */
        public synchronized void destroy(final boolean remove) {
            if (m_instance == null) {
                throw new NullPointerException("The resource has been destroyed already.");
            }

            m_available.set(false);
            m_instance.close();

            if (remove) {
                try (LockHolder lock = new LockHolder(RESOURCES_LOCK)) {
                    RESOURCES.get(m_preferences).remove(this);
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
            return (m_instance != null) && m_instance.isAlive();
        }

    }

}
