/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.r;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.util.FileUtil;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadUtils;
import org.knime.core.webui.node.dialog.scripting.ScriptingService;
import org.knime.core.webui.node.dialog.scripting.lsp.LanguageServerProxy;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.r.controller.ConsoleLikeRExecutor;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;

/**
 * {@link ScriptingService} implementation for the interactive R scripting dialog. Manages a live R session via
 * {@link RController} and exposes RPC methods for running scripts, resetting the workspace, and killing the session.
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
class RScriptingService extends ScriptingService {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RScriptingService.class);

    /** Default batch size for transferring KNIME tables to R (matches {@link RSnippetSettings} default). */
    private static final int DEFAULT_BATCH_SIZE = 10000;

    /** Default R type for the knime.in variable (matches {@link RSnippetSettings} default). */
    private static final String DEFAULT_KNIME_IN_TYPE = "data.frame";

    /** Default for whether to send row names to R (matches {@link RSnippetSettings} default). */
    private static final boolean DEFAULT_SEND_ROW_NAMES = true;

    private final AtomicBoolean m_expectCancel = new AtomicBoolean(false);

    /** The active R controller – null when no session is running. */
    private RController m_controller;

    /**
     * A {@link LanguageServerStarter} that closes the previous R process before starting a new one.
     * <p>
     * {@code connectToLanguageServer()} is called on every dialog page load. Without this guard, each reload
     * would start a second R languageserver process (the framework replaces {@code m_languageServer} but never
     * closes the previous one). The synchronized {@code start()} method ensures atomic close-then-start.
     */
    private static final class ClosePreviousOnRestartStarter implements LanguageServerStarter {
        private LanguageServerProxy m_proxy;

        @Override
        public synchronized LanguageServerProxy start() throws IOException {
            if (m_proxy != null) {
                m_proxy.close();
                m_proxy = null;
            }
            m_proxy = RLanguageServer.startLanguageServer();
            return m_proxy;
        }
    }

    /**
     * Creates a new service that starts the R language server ({@code languageserver::run()}) when the dialog
     * connects. Falls back gracefully if the {@code languageserver} R package is not available.
     */
    RScriptingService() {
        super(new ClosePreviousOnRestartStarter(), x -> true);
    }

    @Override
    public RRpcService getJsonRpcService() {
        return new RRpcService();
    }

    /**
     * Called when the dialog page is temporarily deactivated (e.g. when the UI framework re-requests initial
     * data after a settings change). We intentionally do NOT close the language server here — it must survive
     * page reloads. The R execution session is cleared because a fresh run-script cycle will start anyway.
     */
    @Override
    public void onDeactivate() {
        // Do NOT call super.onDeactivate() — that would close the language server proxy.
        // The proxy's lifecycle is managed by ClosePreviousOnRestartStarter and onDialogClose().
        clearSession();
    }

    /**
     * Called when the dialog is conclusively disposed (registered as {@code onDispose} on the RPC data service).
     * Unlike {@link #onDeactivate()}, this performs a full shutdown: language server, event queue, executor.
     */
    void onDialogClose() {
        super.onDeactivate(); // closes language server proxy, clears event queue, shuts down executor
        clearSession();
    }

    // -----------------------------------------------------------------------
    // Session management helpers
    // -----------------------------------------------------------------------

    private synchronized void clearSession() {
        if (m_controller != null) {
            try {
                m_controller.close();
            } catch (Exception ex) { // NOSONAR
                LOGGER.warn("Failed to close RController: " + ex.getMessage(), ex);
            }
            m_controller = null;
        }
    }

    /**
     * Starts a fresh R session. Any previously running session is closed first. Does NOT import input data or flow
     * variables – call {@link #setupWorkspace(RController, ExecutionMonitor)} for that.
     */
    private synchronized RController startNewSession() throws RException {
        clearSession();
        m_controller = new RController(true, RPreferenceInitializer.getRProvider());
        m_controller.initialize();
        return m_controller;
    }

    /**
     * Returns the existing session controller, or starts a new one if none is active.
     */
    private synchronized RController getOrStartSession() throws RException {
        if (m_controller == null) {
            m_controller = new RController(true, RPreferenceInitializer.getRProvider());
            m_controller.initialize();
        }
        return m_controller;
    }

    /**
     * Imports input data and flow variables into the given R controller. Mirrors the logic in
     * {@code RSnippetNodePanel.resetWorkspace()}.
     */
    private void setupWorkspace(final RController controller, final ExecutionMonitor exec)
        throws RException, CanceledExecutionException {
        final PortObject[] inputData = getWorkflowControl().getInputData();
        if (inputData != null) {
            exec.setMessage("Sending input data to R");
            controller.importDataFromPorts(inputData, exec.createSubProgress(0.7), DEFAULT_BATCH_SIZE,
                DEFAULT_KNIME_IN_TYPE, DEFAULT_SEND_ROW_NAMES);
        }

        exec.setMessage("Sending flow variables to R");
        final Collection<FlowVariable> flowVars = Optional
            .ofNullable(getWorkflowControl().getFlowObjectStack())
            .map(stack -> stack.getAllAvailableFlowVariables().values())
            .orElseGet(java.util.Collections::emptyList);
        controller.exportFlowVariables(flowVars, "knime.flow.in", exec.createSubProgress(0.3));
    }

    // -----------------------------------------------------------------------
    // Execution helpers
    // -----------------------------------------------------------------------

    private ExecutionMonitor newExec() {
        return new ExecutionMonitor(new DefaultNodeProgressMonitor());
    }

    /**
     * Normalizes script text from the frontend by stripping Windows-style carriage returns that would cause R parse
     * errors (e.g. "unexpected input" at the position of the hidden \r character).
     */
    private static String normalizeScript(final String script) {
        return script == null ? "" : script.replace("\r", "");
    }

    /**
     * Returns an R script that calls {@code png(path, width, height)} to redirect graphics output to a file instead
     * of opening a native window. To be prepended to the user script.
     */
    private static String buildPngSetupScript(final File plotFile) {
        final String path = plotFile.getAbsolutePath().replace('\\', '/');
        return "png(\"" + path + "\", width=800L, height=600L)\n";
    }

    /**
     * Silently runs a short R command on the given controller, discarding all output. Used for cleanup commands such
     * as {@code dev.off()} that should always run but whose output is not user-visible.
     */
    private void executeRQuietly(final RController controller, final String rCode) {
        try {
            final ConsoleLikeRExecutor ex = new ConsoleLikeRExecutor(controller);
            final ExecutionMonitor mon = newExec();
            ex.setupOutputCapturing(mon);
            ex.executeIgnoreResult(rCode, mon);
            ex.finishOutputCapturing(mon);
            ex.cleanup(mon);
        } catch (Exception ignored) { // NOSONAR – best-effort cleanup
        }
    }

    /**
     * Runs the given R script on the given controller and emits console events for all output.
     *
     * @param controller the active R controller
     * @param script the R code to execute
     * @param exec execution monitor
     * @throws RException if R reports an error
     * @throws CanceledExecutionException if execution is cancelled
     */
    private void executeScript(final RController controller, final String script, final ExecutionMonitor exec)
        throws RException, CanceledExecutionException, InterruptedException {

        final ConsoleLikeRExecutor executor = new ConsoleLikeRExecutor(controller);

        exec.setMessage("Setting up output capturing");
        executor.setupOutputCapturing(exec);

        exec.setMessage("Executing R script");
        executor.executeIgnoreResult(script, exec);

        exec.setMessage("Collecting output");
        executor.finishOutputCapturing(exec);

        // Emit stdout/stderr as console events
        if (!executor.getStdOut().isEmpty()) {
            addConsoleOutputEvent(new ConsoleText(executor.getStdOut(), false));
        }
        if (!executor.getStdErr().isEmpty()) {
            addConsoleOutputEvent(new ConsoleText(executor.getStdErr(), true));
        }

        exec.setMessage("Cleaning up");
        executor.cleanup(exec);

        // Propagate R-level errors
        final boolean isError = executor.getStdErr().lines()
            .anyMatch(line -> line.startsWith(ConsoleLikeRExecutor.ERROR_PREFIX));
        if (isError) {
            throw new RException("R error:\n" + executor.getStdErr(), null);
        }
    }

    private void executeInBackground(final String script, final boolean newSession) {
        m_expectCancel.set(false);
        ThreadUtils.threadWithContext(() -> {
            final RController controller;
            try {
                if (newSession) {
                    controller = startNewSession();
                    // Always set up workspace when starting a new session
                    final ExecutionMonitor setupExec = newExec();
                    setupWorkspace(controller, setupExec);
                } else {
                    controller = getOrStartSession();
                }
            } catch (Exception ex) { // NOSONAR
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                final String msg = "Failed to start R session: " + ex.getMessage();
                LOGGER.error(msg, ex);
                sendExecutionFinishedEvent(ExecutionStatus.ERROR, msg);
                return;
            }

            // Create a temp PNG file so that plots are captured to disk instead of opening a native window.
            File plotTempDir = null;
            File plotFile = null;
            try {
                plotTempDir = FileUtil.createTempDir("r-scripting-plot-");
                plotFile = new File(plotTempDir, "plot.png");
            } catch (IOException ioEx) {
                LOGGER.warn("Could not create temp dir for plot capture – plots may open as native windows.", ioEx);
            }

            // Prepend png() device setup so that any plot() calls write to the temp file.
            final String scriptWithPlot = plotFile != null
                ? buildPngSetupScript(plotFile) + script
                : script;

            try {
                final ExecutionMonitor exec = newExec();
                addConsoleOutputEvent(new ConsoleText("Running script...\n", false));
                executeScript(controller, scriptWithPlot, exec);
                addConsoleOutputEvent(new ConsoleText("Script finished.\n", false));
                sendExecutionFinishedEvent(ExecutionStatus.SUCCESS, null);
            } catch (CanceledExecutionException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                sendExecutionFinishedEvent(ExecutionStatus.CANCELLED, "Script execution was cancelled.");
            } catch (Exception ex) { // NOSONAR
                if (m_expectCancel.get()) {
                    sendExecutionFinishedEvent(ExecutionStatus.CANCELLED, "Script execution was cancelled.");
                } else {
                    final String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    LOGGER.error("R script execution failed: " + msg, ex);
                    sendExecutionFinishedEvent(ExecutionStatus.ERROR, msg);
                }
            } finally {
                m_expectCancel.set(false);

                // Always close the PNG device (safe even if script failed or no device was opened).
                if (plotFile != null) {
                    executeRQuietly(controller, "tryCatch(dev.off(), error=function(e){})");

                    // Emit the captured plot to the frontend if the PNG has content.
                    try {
                        if (plotFile.exists() && plotFile.length() > 0) {
                            final byte[] pngBytes = Files.readAllBytes(plotFile.toPath());
                            final String base64Png = Base64.getEncoder().encodeToString(pngBytes);
                            sendEvent("r-plot", base64Png);
                        }
                    } catch (IOException ioEx) {
                        LOGGER.warn("Could not read plot PNG file: " + ioEx.getMessage());
                    } finally {
                        FileUtil.deleteRecursively(plotTempDir);
                    }
                }
            }
        }, "r-execution").start();
    }

    private void sendExecutionFinishedEvent(final ExecutionStatus status, final String message) {
        sendEvent("r-execution-finished", new ExecutionFinishedInfo(status, message));
    }

    // -----------------------------------------------------------------------
    // Public enums / records (used in event payload – must be public for JSON-RPC serialization)
    // -----------------------------------------------------------------------

    /** Execution result status sent back to the frontend. */
    public enum ExecutionStatus {
            SUCCESS, ERROR, CANCELLED
    }

    /** Payload for the {@code r-execution-finished} event. */
    public static final class ExecutionFinishedInfo {
        public final ExecutionStatus status; // NOSONAR – public for JSON serialization

        public final String message; // NOSONAR

        ExecutionFinishedInfo(final ExecutionStatus status, final String message) {
            this.status = status;
            this.message = message;
        }
    }

    // -----------------------------------------------------------------------
    // RPC service (public methods are called from the frontend via JSON-RPC)
    // -----------------------------------------------------------------------

    /**
     * The JSON-RPC service exposed to the frontend.
     *
     * <p>NB: Must be public for the JSON-RPC server.
     */
    public final class RRpcService extends RpcService {

        /**
         * Run the given R script in a fresh session (workspace is reset first, input data is re-imported).
         *
         * @param script the R code entered by the user
         */
        public void runScript(final String script) {
            executeInBackground(normalizeScript(script), true);
        }

        /**
         * Run the given R script in the existing R session, if one is already running. If not, starts a new session.
         *
         * @param script the R code to execute
         */
        public void runInExistingSession(final String script) {
            executeInBackground(normalizeScript(script), false);
        }

        /**
         * Reset the R workspace: clear all variables, re-import input data and flow variables.
         */
        public void resetWorkspace() {
            m_expectCancel.set(false);
            ThreadUtils.threadWithContext(() -> {
                final ExecutionMonitor exec = newExec();
                try {
                    final RController controller = getOrStartSession();
                    exec.setMessage("Clearing workspace");
                    controller.clearWorkspace(exec.createSubProgress(0.3));
                    setupWorkspace(controller, exec.createSubProgress(0.7));
                    addConsoleOutputEvent(new ConsoleText("Workspace reset successfully.\n", false));
                    sendExecutionFinishedEvent(ExecutionStatus.SUCCESS, null);
                } catch (Exception ex) { // NOSONAR
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    final String msg = "Failed to reset workspace: " + ex.getMessage();
                    LOGGER.error(msg, ex);
                    addConsoleOutputEvent(new ConsoleText(msg + "\n", true));
                    sendExecutionFinishedEvent(ExecutionStatus.ERROR, msg);
                }
            }, "r-reset-workspace").start();
        }

        /**
         * Kill the running R session. Safe to call when no session is active.
         */
        public void killSession() {
            m_expectCancel.set(true);
            ThreadUtils.threadWithContext(() -> {
                clearSession();
                addConsoleOutputEvent(new ConsoleText("R session stopped.\n", true));
                sendExecutionFinishedEvent(ExecutionStatus.CANCELLED, "Session killed by user.");
            }, "r-kill-session").start();
        }

        /**
         * Returns a human-readable string describing the configured R installation (path and version). Intended for
         * display in the script console as a diagnostic aid.
         *
         * @return e.g. {@code "R version 4.5.2 (2025-10-31) — C:/Program Files/R/R-4.5.2/bin/Rscript.exe"}
         */
        public String getRInfo() {
            final String rscriptPath = RPreferenceInitializer.getRProvider().getRBinPath("Rscript");
            if (rscriptPath == null || rscriptPath.isBlank()) {
                return "R not configured — set the path in KNIME Preferences → KNIME → R.";
            }
            try {
                final Process p = new ProcessBuilder(rscriptPath, "--no-save", "--no-restore", "--slave",
                    "-e", "writeLines(R.version.string)").start();
                final String version = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                p.waitFor();
                return version + " — " + rscriptPath;
            } catch (final Exception e) {
                return rscriptPath + " (could not determine version: " + e.getMessage() + ")";
            }
        }

        @Override
        protected org.knime.core.webui.node.dialog.scripting.CodeGenerationRequest getCodeSuggestionRequest(
            final String userPrompt, final String currentCode,
            final org.knime.core.webui.node.dialog.scripting.InputOutputModel[] inputOutputModels) {
            throw new UnsupportedOperationException("Code generation is not yet supported for R nodes.");
        }
    }
}
