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
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadUtils;
import org.knime.core.webui.node.dialog.scripting.CodeGenerationRequest;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel;
import org.knime.core.webui.node.dialog.scripting.ScriptingService;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.r.controller.ConsoleLikeRExecutor;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;

/**
 * {@link ScriptingService} implementation for the interactive R scripting dialog. Manages a live R session via
 * {@link RController} and exposes RPC methods for running scripts, resetting the workspace, and killing the session.
 *
 * @author Marc Lehner, KNIME GmbH, Zurich, Switzerland
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
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

    /**
     * Set to {@code true} just before calling {@code killSession()} so that an {@link RException} thrown by a
     * concurrent {@code executeScript} call is treated as a cancellation rather than an error.
     */
    private final AtomicBoolean m_expectCancel = new AtomicBoolean(false);

    /** The active R controller – null when no session is running. */
    private RController m_controller;

    RScriptingService() {
        super();
    }

    @Override
    public RRpcService getJsonRpcService() {
        return new RRpcService();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        clearSession();
    }

    // -----------------------------------------------------------------------
    // Session management
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

    /** Starts a fresh R session. Any previously running session is closed first. */
    private synchronized RController startNewSession() throws RException {
        clearSession();
        m_controller = new RController(true, RPreferenceInitializer.getRProvider());
        m_controller.initialize();
        return m_controller;
    }

    /** Returns the existing session controller, or starts a new one if none is active. */
    private synchronized RController getOrStartSession() throws RException {
        if (m_controller == null) {
            m_controller = new RController(true, RPreferenceInitializer.getRProvider());
            m_controller.initialize();
        }
        return m_controller;
    }

    /**
     * Imports input data and flow variables into the given R controller, making {@code knime.in} and
     * {@code knime.flow.in} available in the R workspace.
     */
    private void setupWorkspace(final RController controller, final ExecutionMonitor exec)
        throws RException, CanceledExecutionException {
        final var inputData = getWorkflowControl().getInputData();
        if (inputData != null) {
            exec.setMessage("Sending input data to R");
            controller.importDataFromPorts(inputData, exec.createSubProgress(0.7), DEFAULT_BATCH_SIZE,
                DEFAULT_KNIME_IN_TYPE, DEFAULT_SEND_ROW_NAMES);
        }

        exec.setMessage("Sending flow variables to R");
        final Collection<FlowVariable> flowVars = Optional //
            .ofNullable(getWorkflowControl().getFlowObjectStack()) //
            .map(stack -> stack.getAllAvailableFlowVariables().values()) //
            .orElseGet(java.util.Collections::emptyList);
        controller.exportFlowVariables(flowVars, "knime.flow.in", exec.createSubProgress(0.3));
    }

    // -----------------------------------------------------------------------
    // Execution helpers
    // -----------------------------------------------------------------------

    private static ExecutionMonitor newExec() {
        return new ExecutionMonitor(new DefaultNodeProgressMonitor());
    }

    /**
     * Returns an R script that redirects graphics output to a PNG file instead of opening a native window.
     */
    private static String buildPngSetupScript(final File plotFile) {
        final String path = plotFile.getAbsolutePath().replace('\\', '/');
        return "png(\"" + path + "\", width=800L, height=600L)\n";
    }

    /**
     * Silently runs a short R command, discarding output. Used for best-effort cleanup (e.g. {@code dev.off()}).
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
     * Runs the given R script and emits console output events for stdout/stderr. Throws {@link RException} if R
     * reports an error.
     */
    private void executeScript(final RController controller, final String script, final ExecutionMonitor exec)
        throws RException, CanceledExecutionException, InterruptedException {
        final ConsoleLikeRExecutor executor = new ConsoleLikeRExecutor(controller);
        executor.setupOutputCapturing(exec);
        executor.executeIgnoreResult(script, exec);
        executor.finishOutputCapturing(exec);

        if (!executor.getStdOut().isEmpty()) {
            addConsoleOutputEvent(new ConsoleText(executor.getStdOut(), false));
        }
        if (!executor.getStdErr().isEmpty()) {
            addConsoleOutputEvent(new ConsoleText(executor.getStdErr(), true));
        }

        executor.cleanup(exec);

        final boolean isError =
            executor.getStdErr().lines().anyMatch(line -> line.startsWith(ConsoleLikeRExecutor.ERROR_PREFIX));
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
                    setupWorkspace(controller, newExec());
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
            final String scriptWithPlot = plotFile != null ? buildPngSetupScript(plotFile) + script : script;

            try {
                addConsoleOutputEvent(new ConsoleText("Running script...\n", false));
                executeScript(controller, scriptWithPlot, newExec());
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
                if (plotFile != null) {
                    executeRQuietly(controller, "tryCatch(dev.off(), error=function(e){})");
                    try {
                        if (plotFile.exists() && plotFile.length() > 0) {
                            final byte[] pngBytes = Files.readAllBytes(plotFile.toPath());
                            sendEvent("r-plot", Base64.getEncoder().encodeToString(pngBytes));
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
    // Event payload types
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
         * @param script the R code to execute
         */
        public void runScript(final String script) {
            executeInBackground(script, true);
        }

        /**
         * Run the given R script in the existing R session (or starts one if none is active).
         *
         * @param script the R code to execute
         */
        public void runInExistingSession(final String script) {
            executeInBackground(script, false);
        }

        /**
         * Reset the R workspace: clear all R variables and re-import input data and flow variables.
         */
        public void resetWorkspace() {
            m_expectCancel.set(false);
            ThreadUtils.threadWithContext(() -> {
                final ExecutionMonitor exec = newExec();
                try {
                    final RController controller = getOrStartSession();
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

        @Override
        protected CodeGenerationRequest getCodeSuggestionRequest(final String userPrompt, final String currentCode,
            final InputOutputModel[] inputOutputModels) {
            throw new UnsupportedOperationException("Code generation is not yet supported for R nodes.");
        }
    }
}
