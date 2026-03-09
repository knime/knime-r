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
         * URI of the currently open document, captured from {@code textDocument/didOpen}.
         * Used when rebuilding {@code textDocument/didChange} messages.
         */
        private String m_lspDocumentUri = null;

        /**
         * Full content of the document as maintained by this class (LF line endings).
         * Initialized from {@code textDocument/didOpen} and updated by each
         * {@code textDocument/didChange} by applying the incremental deltas.
         */
        private String m_lspDocumentContent = "";

        /**
         * The last version number sent to the R language server in a {@code textDocument/didChange}
         * notification. Used to generate synthetic version numbers for temporary document patches.
         */
        private int m_lspVersion = 0;

        /**
         * Intercepts outgoing LSP messages to adapt them for the R {@code languageserver} package.
         *
         * <p><b>Why this override is necessary:</b><br>
         * The R {@code languageserver} package advertises {@code "textDocumentSync":{"change":1}} in its
         * {@code initialize} response, meaning it expects <em>full document content</em> in every
         * {@code textDocument/didChange} notification (LSP spec §3.17 TextDocumentSyncKind.Full).
         * However, {@code @knime/scripting-editor}'s {@code MonacoLSPConnection} always sends
         * <em>incremental</em> changes (with a {@code range} property), regardless of the server's
         * advertised capability. The R handler reads only {@code contentChanges[[1]]$text} and treats it
         * as the new complete document — so an incremental delta like {@code "\n"} (one line break)
         * replaces the entire document, breaking all subsequent hover and completion requests.
         *
         * <p><b>Fix:</b><br>
         * This override maintains a Java-side replica of the document. On {@code didOpen} it initialises
         * the replica and normalises CRLF to LF. On {@code didChange} it applies each incremental delta
         * to the replica, then rebuilds the message with the full document content (no {@code range}),
         * which is what the R languageserver expects.
         */
        @Override
        public void sendLanguageServerMessage(final String message) {
            if (message.contains("\"textDocument/didOpen\"")) {
                super.sendLanguageServerMessage(processDidOpen(message));
            } else if (message.contains("\"textDocument/didChange\"")) {
                super.sendLanguageServerMessage(processDidChange(message));
            } else if (message.contains("\"textDocument/completion\"")) {
                handleCompletion(message);
            } else {
                super.sendLanguageServerMessage(message);
            }
        }

        /**
         * Handles a {@code textDocument/didOpen} message: extracts and stores the document URI and
         * full text (normalising CRLF to LF), and returns the message with the normalised text.
         */
        private String processDidOpen(final String message) {
            final String rawText = extractJsonStrValue(message, "\"text\":\"");
            if (rawText == null) {
                LOGGER.warn("[R-LSP] didOpen: could not extract text field – forwarding unchanged");
                return message;
            }
            final String decoded = jsonDecode(rawText);
            final String normalized = decoded.replace("\r\n", "\n").replace("\r", "\n");
            m_lspDocumentContent = normalized;

            final String rawUri = extractJsonStrValue(message, "\"uri\":\"");
            if (rawUri != null) {
                m_lspDocumentUri = rawUri; // URIs contain no JSON escape sequences in practice
            }

            // Rebuild with normalised text so R also gets LF-only content in didOpen
            return replaceJsonStrValue(message, "\"text\":\"", jsonEncode(normalized));
        }

        /**
         * Handles a {@code textDocument/didChange} message: applies each incremental content change to
         * {@link #m_lspDocumentContent} and rebuilds the message with a single full-document
         * {@code contentChanges} entry (no {@code range}), as required by R {@code languageserver}.
         */
        private String processDidChange(final String message) {
            final int arrayStart = message.indexOf("\"contentChanges\":[");
            if (arrayStart < 0) {
                LOGGER.warn("[R-LSP] didChange: no contentChanges array – forwarding unchanged");
                return message;
            }

            // Walk each change object in the array and apply it to m_lspDocumentContent
            int pos = arrayStart + "\"contentChanges\":[".length();
            while (pos < message.length()) {
                while (pos < message.length() && message.charAt(pos) <= ' ') {
                    pos++;
                }
                if (pos >= message.length() || message.charAt(pos) != '{') {
                    break;
                }
                final int objEnd = findObjectEnd(message, pos);
                if (objEnd < 0) {
                    break;
                }
                final String changeObj = message.substring(pos, objEnd + 1);
                final String rawChangeText = extractJsonStrValue(changeObj, "\"text\":\"");
                if (rawChangeText != null) {
                    final String changeText = jsonDecode(rawChangeText)
                        .replace("\r\n", "\n").replace("\r", "\n");
                    if (changeObj.contains("\"range\":")) {
                        final int rangeStart = changeObj.indexOf("\"range\":{");
                        final int rangeObjStart = changeObj.indexOf('{', rangeStart + 8);
                        final int rangeObjEnd = findObjectEnd(changeObj, rangeObjStart);
                        if (rangeObjEnd > rangeObjStart) {
                            final String rangeObj = changeObj.substring(rangeObjStart, rangeObjEnd + 1);
                            final int startLine = extractIntAfterKeys(rangeObj, "\"start\"", "\"line\":");
                            final int startChar = extractIntAfterKeys(rangeObj, "\"start\"", "\"character\":");
                            final int endLine = extractIntAfterKeys(rangeObj, "\"end\"", "\"line\":");
                            final int endChar = extractIntAfterKeys(rangeObj, "\"end\"", "\"character\":");
                            m_lspDocumentContent = applyLspChange(
                                m_lspDocumentContent, startLine, startChar, endLine, endChar, changeText);
                        }
                    } else {
                        // Full-document replacement (no range) – store directly
                        m_lspDocumentContent = changeText;
                    }
                }
                pos = objEnd + 1;
                while (pos < message.length()
                    && (message.charAt(pos) == ',' || message.charAt(pos) <= ' ')) {
                    pos++;
                }
            }

            // Rebuild the didChange message with the full document content
            final int version = extractVersionInt(message);
            if (version > m_lspVersion) {
                m_lspVersion = version;
            }
            final String uri = m_lspDocumentUri != null ? m_lspDocumentUri : "inmemory://model/script.R";
            return "{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didChange\",\"params\":{"
                + "\"textDocument\":{\"uri\":\"" + jsonEncode(uri) + "\",\"version\":" + version + "},"
                + "\"contentChanges\":[{\"text\":\"" + jsonEncode(m_lspDocumentContent) + "\"}]}}";
        }

        /**
         * Handles a {@code textDocument/completion} request.
         * <p>
         * R {@code languageserver}'s {@code completion_reply} only returns completions when the token
         * at the cursor is non-empty ({@code nzchar(full_token)} guard). On an empty line at
         * {@code character:0}, the token is {@code ""} and R returns {@code {"items":[]}} — the user sees
         * nothing when pressing Ctrl+Space on a blank line.
         * <p>
         * When a manually-triggered completion ({@code triggerKind:1}) arrives at {@code character:0}
         * and our document replica shows an empty line there, this method:
         * <ol>
         *   <li>Temporarily patches the document by inserting the letter {@code "k"} at the cursor
         *       position and sends the patched document to R as a {@code didChange}.</li>
         *   <li>Forwards the completion request with {@code character:1} (cursor now after "k"),
         *       so R computes completions for the "k" prefix — returning {@code knime.in},
         *       {@code knime.out}, {@code knime.flow.in} and other "k*" identifiers from the
         *       document and installed packages.</li>
         *   <li>Immediately sends another {@code didChange} to restore the original document in R,
         *       so all subsequent hover and completion requests see the correct document state.</li>
         * </ol>
         * R processes messages sequentially, so the triplet (patch → completion → restore) is
         * atomic from the language server's perspective.
         */
        private void handleCompletion(final String message) {
            final int character = extractIntAfterKeys(message, "\"position\"", "\"character\":");

            // Only intercept manual Ctrl+Space (triggerKind:1) at character:0 on an empty line.
            // For all other completions (typing, re-trigger) forward as-is.
            if (character != 0 || !message.contains("\"triggerKind\":1")) {
                super.sendLanguageServerMessage(message);
                return;
            }
            final int line = extractIntAfterKeys(message, "\"position\"", "\"line\":");
            final String[] lines = m_lspDocumentContent.split("\n", -1);
            final boolean lineIsEmpty = (line >= lines.length) || lines[line].isEmpty();
            if (!lineIsEmpty) {
                super.sendLanguageServerMessage(message);
                return;
            }

            // Build the patched content: insert "k" at the start of the empty line.
            final StringBuilder sb = new StringBuilder(m_lspDocumentContent.length() + 1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                if (i == line) {
                    sb.append('k');
                }
                if (i < lines.length) {
                    sb.append(lines[i]);
                }
            }
            final String patchedContent = sb.toString();

            final String uri = m_lspDocumentUri != null ? m_lspDocumentUri : "inmemory://model/script.R";
            final String uriEncoded = jsonEncode(uri);

            // Step 1: send didChange with "k" on the empty line so R has a non-empty token.
            final int patchVersion = ++m_lspVersion;
            super.sendLanguageServerMessage(
                "{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didChange\",\"params\":{"
                    + "\"textDocument\":{\"uri\":\"" + uriEncoded + "\",\"version\":" + patchVersion + "},"
                    + "\"contentChanges\":[{\"text\":\"" + jsonEncode(patchedContent) + "\"}]}}");

            // Step 2: forward the completion request with character:1 (cursor after "k").
            super.sendLanguageServerMessage(message.replace("\"character\":0", "\"character\":1"));

            // Step 3: immediately restore the original document so hover/completion still works.
            final int restoreVersion = ++m_lspVersion;
            super.sendLanguageServerMessage(
                "{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didChange\",\"params\":{"
                    + "\"textDocument\":{\"uri\":\"" + uriEncoded + "\",\"version\":" + restoreVersion + "},"
                    + "\"contentChanges\":[{\"text\":\"" + jsonEncode(m_lspDocumentContent) + "\"}]}}");
        }

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

        /**
         * Pre-warms the {@code callr} background session used by the R language server for completions.
         * <p>
         * The R language server ({@code languageserver}) uses the {@code callr} package to spawn a
         * background R subprocess for completion evaluation. This subprocess starts lazily on the first
         * {@code textDocument/completion} request and takes 3–10 seconds to initialize. During that time
         * any Ctrl+Space request returns an empty list, which the user experiences as "completions not
         * working".
         * <p>
         * This method sends a dummy completion request 300 ms after the LSP has connected, which starts
         * the callr subprocess in the background. By the time the user presses Ctrl+Space for the first
         * time, callr is already running and can return real completions.
         * <p>
         * The request uses ID 99999, which is not tracked by the frontend's vscode-languageserver-protocol
         * client. When R responds, the client discards the message with a console warning
         * ("Received response message without active request") — this is harmless; the connection stays
         * intact.
         */
        public void warmUpLanguageServer() {
            ThreadUtils.threadWithContext(() -> {
                try {
                    Thread.sleep(300);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                sendLanguageServerMessage(
                    "{\"jsonrpc\":\"2.0\",\"id\":99999,\"method\":\"textDocument/completion\","
                        + "\"params\":{\"textDocument\":{\"uri\":\"inmemory://model/script.R\"},"
                        + "\"position\":{\"line\":0,\"character\":0},"
                        + "\"context\":{\"triggerKind\":1}}}");
                LOGGER.debug("Sent warm-up completion request to R language server to pre-start callr.");
            }, "r-lsp-warmup").start();
        }

        @Override
        protected org.knime.core.webui.node.dialog.scripting.CodeGenerationRequest getCodeSuggestionRequest(
            final String userPrompt, final String currentCode,
            final org.knime.core.webui.node.dialog.scripting.InputOutputModel[] inputOutputModels) {
            throw new UnsupportedOperationException("Code generation is not yet supported for R nodes.");
        }
    }

    // -----------------------------------------------------------------------
    // Static helpers for LSP JSON manipulation (no external library required)
    // -----------------------------------------------------------------------

    /**
     * Extracts the raw (JSON-escape-preserved) content of the first JSON string value identified by
     * {@code prefix} (e.g. {@code "\"text\":\""}) from {@code json}. Returns the raw escaped content
     * between the opening and closing {@code "}, or {@code null} if the prefix is not found or the string
     * is unterminated.
     */
    static String extractJsonStrValue(final String json, final String prefix) {
        final int start = json.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int i = start + prefix.length();
        final StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            final char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                sb.append('\\');
                sb.append(json.charAt(i + 1));
                i += 2;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return null;
    }

    /**
     * Replaces the raw content of the first JSON string value identified by {@code prefix} with
     * {@code newRawValue} (which should already be JSON-encoded). Returns the original string unchanged
     * if the prefix is not found.
     */
    static String replaceJsonStrValue(final String json, final String prefix, final String newRawValue) {
        final int prefixEnd = json.indexOf(prefix);
        if (prefixEnd < 0) {
            return json;
        }
        final int valueStart = prefixEnd + prefix.length();
        boolean escape = false;
        for (int i = valueStart; i < json.length(); i++) {
            final char c = json.charAt(i);
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return json.substring(0, valueStart) + newRawValue + json.substring(i);
            }
        }
        return json;
    }

    /**
     * JSON-decodes a raw escaped string value (the content between the {@code "} delimiters of a JSON
     * string, as returned by {@link #extractJsonStrValue}). Handles the standard JSON escape sequences
     * {@code \"}, {@code \\}, {@code \/}, {@code \n}, {@code \r}, {@code \t}, {@code \b}, {@code \f},
     * and Unicode escapes of the form backslash-u followed by four hex digits.
     */
    static String jsonDecode(final String raw) {
        final StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            final char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                i++;
                switch (raw.charAt(i)) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        if (i + 4 < raw.length()) {
                            sb.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(raw.charAt(i));
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * JSON-encodes a Java {@link String} so that it is safe to embed as the value of a JSON string
     * literal (without the surrounding {@code "} delimiters). Escapes {@code "}, {@code \}, {@code \n},
     * {@code \r}, {@code \t}, {@code \b}, {@code \f}, and control characters below U+0020.
     */
    static String jsonEncode(final String value) {
        final StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Finds the index of the closing {@code '}'} that matches the {@code '{'} at {@code pos} in
     * {@code json}, correctly handling nested objects/arrays and string literals. Returns {@code -1} if
     * no matching brace is found.
     */
    static int findObjectEnd(final String json, final int pos) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = pos; i < json.length(); i++) {
            final char c = json.charAt(i);
            if (escape) {
                escape = false;
            } else if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Extracts the integer value of a JSON field after {@code parentKey} and then {@code childKey} within
     * the given {@code json} fragment. This two-step search avoids false matches from unrelated fields
     * that share the same {@code childKey}. Returns {@code 0} if not found.
     * <p>
     * Example: {@code extractIntAfterKeys(rangeObj, "\"start\"", "\"line\":")} returns the value of
     * {@code start.line} in a range JSON object.
     */
    static int extractIntAfterKeys(final String json, final String parentKey, final String childKey) {
        final int parentIdx = json.indexOf(parentKey);
        if (parentIdx < 0) {
            return 0;
        }
        final int childIdx = json.indexOf(childKey, parentIdx);
        if (childIdx < 0) {
            return 0;
        }
        int numStart = childIdx + childKey.length();
        while (numStart < json.length() && json.charAt(numStart) == ' ') {
            numStart++;
        }
        int numEnd = numStart;
        while (numEnd < json.length() && Character.isDigit(json.charAt(numEnd))) {
            numEnd++;
        }
        if (numEnd == numStart) {
            return 0;
        }
        return Integer.parseInt(json.substring(numStart, numEnd));
    }

    /**
     * Extracts the {@code version} integer from the {@code textDocument} object of a
     * {@code textDocument/didChange} message. Returns {@code -1} if not found.
     */
    static int extractVersionInt(final String message) {
        final int vIdx = message.indexOf("\"version\":");
        if (vIdx < 0) {
            return -1;
        }
        int numStart = vIdx + 10;
        while (numStart < message.length() && message.charAt(numStart) == ' ') {
            numStart++;
        }
        int numEnd = numStart;
        while (numEnd < message.length() && Character.isDigit(message.charAt(numEnd))) {
            numEnd++;
        }
        if (numEnd == numStart) {
            return -1;
        }
        return Integer.parseInt(message.substring(numStart, numEnd));
    }

    /**
     * Applies a single incremental LSP text change to {@code document} (which must use LF-only line
     * endings). The method replaces the character range
     * [{@code startLine}:{@code startChar} — {@code endLine}:{@code endChar}] with {@code newText}.
     * All positions are 0-based. Line lengths are measured in UTF-16 code units (characters), consistent
     * with the LSP specification.
     *
     * @param document the current document content (LF line endings)
     * @param startLine 0-based line index of the start of the replaced range
     * @param startChar 0-based character offset on {@code startLine}
     * @param endLine 0-based line index of the end of the replaced range
     * @param endChar 0-based character offset on {@code endLine}
     * @param newText the replacement text (LF line endings)
     * @return the modified document
     */
    static String applyLspChange(final String document, final int startLine, final int startChar,
            final int endLine, final int endChar, final String newText) {
        final String[] lines = document.split("\n", -1);

        // Compute start offset (byte position in document string)
        int startOffset = 0;
        for (int i = 0; i < Math.min(startLine, lines.length); i++) {
            startOffset += lines[i].length() + 1; // +1 for the \n separator
        }
        if (startLine < lines.length) {
            startOffset += Math.min(startChar, lines[startLine].length());
        }

        // Compute end offset
        int endOffset = 0;
        for (int i = 0; i < Math.min(endLine, lines.length); i++) {
            endOffset += lines[i].length() + 1;
        }
        if (endLine < lines.length) {
            endOffset += Math.min(endChar, lines[endLine].length());
        }

        startOffset = Math.min(startOffset, document.length());
        endOffset = Math.min(endOffset, document.length());

        if (startOffset > endOffset) {
            LOGGER.warn("[R-LSP] applyLspChange: startOffset (" + startOffset
                + ") > endOffset (" + endOffset + ") – skipping change");
            return document;
        }

        return document.substring(0, startOffset) + newText + document.substring(endOffset);
    }
}
