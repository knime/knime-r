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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.knime.core.webui.node.dialog.scripting.lsp.LanguageServerProxy;
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

    /** Shared Jackson ObjectMapper for parsing and generating LSP messages. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * A {@link LanguageServerStarter} that closes the previous R process before starting a new one.
     * <p>
     * {@code connectToLanguageServer()} is called on every dialog page load. Without this guard, each reload would
     * start a second R languageserver process. The synchronized {@code start()} method ensures atomic close-then-start.
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

    RScriptingService() {
        super(new ClosePreviousOnRestartStarter(), x -> true);
    }

    @Override
    public RRpcService getJsonRpcService() {
        return new RRpcService();
    }

    /**
     * Called when the dialog page is temporarily deactivated (e.g. when the UI framework re-requests initial data
     * after a settings change). We intentionally do NOT close the language server here — it must survive page reloads.
     * The R execution session is cleared because a fresh run-script cycle will start anyway.
     */
    @Override
    public void onDeactivate() {
        // Do NOT call super.onDeactivate() — that would close the language server proxy.
        clearSession();
    }

    /**
     * Called when the dialog is conclusively disposed. Unlike {@link #onDeactivate()}, this performs a full shutdown:
     * language server, event queue, executor.
     */
    void onDialogClose() {
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

        // -----------------------------------------------------------------------
        // LSP compatibility layer fields
        // -----------------------------------------------------------------------

        /** URI of the currently open document, captured from {@code textDocument/didOpen}. */
        private String m_lspDocumentUri = null;

        /**
         * Java-side replica of the document content (LF line endings only). Kept in sync by applying incremental
         * deltas from each {@code textDocument/didChange}, then sent as full-document to R languageserver (which
         * cannot handle incremental changes correctly — it ignores the range and treats the delta text as the new full
         * document).
         */
        private String m_lspDocumentContent = "";

        /**
         * Version number for {@code textDocument/didChange} notifications. Monotonically increasing, tracked here so
         * synthetic messages (empty-line completion workaround) use correct version numbers.
         */
        private int m_lspVersion = 0;

        /**
         * Intercepts outgoing LSP messages and applies compatibility transformations for R languageserver:
         * <ul>
         *   <li>{@code didOpen}: normalize CRLF→LF, initialize document replica.</li>
         *   <li>{@code didChange}: apply incremental deltas to replica, send full-document update to R.</li>
         *   <li>{@code completion} at empty line (manual trigger): inject temporary "k" prefix to work around R's
         *       {@code nzchar(full_token)} guard that returns no completions on empty lines.</li>
         *   <li>All other messages: forward unchanged.</li>
         * </ul>
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

        private String processDidOpen(final String message) {
            try {
                final JsonNode root = MAPPER.readTree(message);
                final JsonNode params = root.get("params");
                if (params == null) {
                    return message;
                }
                final LspDidOpenParams didOpen = MAPPER.treeToValue(params, LspDidOpenParams.class);
                if (didOpen.textDocument == null || didOpen.textDocument.text == null) {
                    return message;
                }
                m_lspDocumentContent = didOpen.textDocument.text.replace("\r\n", "\n").replace("\r", "\n");
                m_lspDocumentUri = didOpen.textDocument.uri;
                didOpen.textDocument.text = m_lspDocumentContent;
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params", MAPPER.valueToTree(didOpen));
                return MAPPER.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] didOpen: JSON parsing failed – forwarding unchanged", e);
                return message;
            }
        }

        private String processDidChange(final String message) {
            try {
                final JsonNode root = MAPPER.readTree(message);
                final JsonNode params = root.get("params");
                if (params == null) {
                    return message;
                }
                final LspDidChangeParams didChange = MAPPER.treeToValue(params, LspDidChangeParams.class);
                if (didChange.contentChanges == null || didChange.contentChanges.isEmpty()) {
                    return message;
                }
                if (didChange.textDocument != null && didChange.textDocument.version > m_lspVersion) {
                    m_lspVersion = didChange.textDocument.version;
                }
                for (final LspContentChange change : didChange.contentChanges) {
                    final String changeText =
                        change.text != null ? change.text.replace("\r\n", "\n").replace("\r", "\n") : "";
                    m_lspDocumentContent = change.range != null
                        ? applyIncrementalChange(m_lspDocumentContent, change.range, changeText) : changeText;
                }
                final LspDidChangeParams fullDocChange = new LspDidChangeParams();
                fullDocChange.textDocument = didChange.textDocument;
                fullDocChange.contentChanges = List.of(createFullDocContentChange(m_lspDocumentContent));
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params",
                    MAPPER.valueToTree(fullDocChange));
                return MAPPER.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] didChange: JSON parsing failed – forwarding unchanged", e);
                return message;
            }
        }

        private String applyIncrementalChange(final String document, final LspRange range, final String newText) {
            final String[] lines = document.split("\n", -1);
            int startOffset = 0;
            for (int i = 0; i < Math.min(range.start.line, lines.length); i++) {
                startOffset += lines[i].length() + 1;
            }
            if (range.start.line < lines.length) {
                startOffset += Math.min(range.start.character, lines[range.start.line].length());
            }
            int endOffset = 0;
            for (int i = 0; i < Math.min(range.end.line, lines.length); i++) {
                endOffset += lines[i].length() + 1;
            }
            if (range.end.line < lines.length) {
                endOffset += Math.min(range.end.character, lines[range.end.line].length());
            }
            startOffset = Math.min(startOffset, document.length());
            endOffset = Math.min(endOffset, document.length());
            if (startOffset > endOffset) {
                LOGGER.warn("[R-LSP] Invalid range: start > end – skipping change");
                return document;
            }
            return document.substring(0, startOffset) + newText + document.substring(endOffset);
        }

        private static LspContentChange createFullDocContentChange(final String text) {
            final LspContentChange change = new LspContentChange();
            change.text = text;
            change.range = null;
            return change;
        }

        /**
         * Handles a {@code textDocument/completion} request. On an empty line with a manual trigger (Ctrl+Space,
         * triggerKind:1), R's completion handler skips all providers due to {@code nzchar(full_token)} being false.
         * Workaround: temporarily inject {@code "k"} at the cursor, request completion at character:1, then restore.
         * R will return all "k*" completions (including {@code knime.in}, {@code knime.flow.in}, etc.).
         */
        private void handleCompletion(final String message) {
            try {
                final JsonNode root = MAPPER.readTree(message);
                final JsonNode params = root.get("params");
                if (params == null) {
                    super.sendLanguageServerMessage(message);
                    return;
                }
                final LspCompletionParams completion = MAPPER.treeToValue(params, LspCompletionParams.class);
                if (completion.position == null || completion.position.character != 0
                    || completion.context == null || completion.context.triggerKind != 1) {
                    super.sendLanguageServerMessage(message);
                    return;
                }
                final String[] lines = m_lspDocumentContent.split("\n", -1);
                final int line = completion.position.line;
                final boolean lineIsEmpty = (line >= lines.length) || lines[line].isEmpty();
                if (!lineIsEmpty) {
                    super.sendLanguageServerMessage(message);
                    return;
                }
                // Build patched content with "k" injected at the start of the empty line
                final StringBuilder sb = new StringBuilder(m_lspDocumentContent.length() + 1);
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) {
                        sb.append('\n');
                    }
                    if (i == line) {
                        sb.append('k');
                    }
                    sb.append(lines[i]);
                }
                final String uri = m_lspDocumentUri != null ? m_lspDocumentUri : "inmemory://model/script.R";
                sendDidChange(uri, ++m_lspVersion, sb.toString());
                completion.position.character = 1;
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params", MAPPER.valueToTree(completion));
                super.sendLanguageServerMessage(MAPPER.writeValueAsString(root));
                sendDidChange(uri, ++m_lspVersion, m_lspDocumentContent);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] completion: JSON parsing failed – forwarding unchanged", e);
                super.sendLanguageServerMessage(message);
            }
        }

        private void sendDidChange(final String uri, final int version, final String content) {
            try {
                final LspDidChangeParams didChange = new LspDidChangeParams();
                didChange.textDocument = new LspVersionedTextDocumentIdentifier();
                didChange.textDocument.uri = uri;
                didChange.textDocument.version = version;
                didChange.contentChanges = List.of(createFullDocContentChange(content));
                final JsonNode root = MAPPER.createObjectNode() //
                    .put("jsonrpc", "2.0") //
                    .put("method", "textDocument/didChange") //
                    .set("params", MAPPER.valueToTree(didChange));
                super.sendLanguageServerMessage(MAPPER.writeValueAsString(root));
            } catch (JsonProcessingException e) {
                LOGGER.error("[R-LSP] Failed to send didChange", e);
            }
        }

        // -----------------------------------------------------------------------
        // RPC methods
        // -----------------------------------------------------------------------

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

        /**
         * Returns a human-readable string describing the configured R installation. Intended for display in the script
         * console as a diagnostic aid.
         *
         * @return e.g. {@code "R version 4.5.2 (2025-10-31) — C:/Program Files/R/R-4.5.2/bin/Rscript.exe"}
         */
        public String getRInfo() {
            final String rscriptPath = RPreferenceInitializer.getRProvider().getRBinPath("Rscript");
            if (rscriptPath == null || rscriptPath.isBlank()) {
                return "R not configured — set the path in KNIME Preferences → KNIME → R.";
            }
            try {
                final Process p = new ProcessBuilder(rscriptPath, "--no-save", "--no-restore", "--slave", //
                    "-e", "writeLines(R.version.string)").start();
                final String version =
                    new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                p.waitFor();
                return version + " — " + rscriptPath;
            } catch (final Exception e) { // NOSONAR
                return rscriptPath + " (could not determine version: " + e.getMessage() + ")";
            }
        }

        @Override
        protected CodeGenerationRequest getCodeSuggestionRequest(final String userPrompt, final String currentCode,
            final InputOutputModel[] inputOutputModels) {
            throw new UnsupportedOperationException("Code generation is not yet supported for R nodes.");
        }
    }

    // -----------------------------------------------------------------------
    // LSP Message POJOs (for Jackson serialization/deserialization)
    // -----------------------------------------------------------------------

    /** LSP {@code textDocument/didOpen} params. */
    static class LspDidOpenParams {
        @JsonProperty public LspTextDocumentItem textDocument;
    }

    /** LSP {@code textDocument/didChange} params. */
    static class LspDidChangeParams {
        @JsonProperty public LspVersionedTextDocumentIdentifier textDocument;
        @JsonProperty public List<LspContentChange> contentChanges;
    }

    /** LSP {@code textDocument/completion} params. */
    static class LspCompletionParams {
        @JsonProperty public LspTextDocumentIdentifier textDocument;
        @JsonProperty public LspPosition position;
        @JsonProperty public LspCompletionContext context;
    }

    /** LSP TextDocumentItem (for {@code didOpen}). */
    static class LspTextDocumentItem {
        @JsonProperty public String uri;
        @JsonProperty public String languageId;
        @JsonProperty public int version;
        @JsonProperty public String text;
    }

    /** LSP VersionedTextDocumentIdentifier. */
    static class LspVersionedTextDocumentIdentifier {
        @JsonProperty public String uri;
        @JsonProperty public int version;
    }

    /** LSP TextDocumentIdentifier. */
    static class LspTextDocumentIdentifier {
        @JsonProperty public String uri;
    }

    /** LSP Position (zero-based line and character). */
    static class LspPosition {
        @JsonProperty public int line;
        @JsonProperty public int character;
    }

    /** LSP ContentChange. If {@code range} is null, replaces the entire document. */
    static class LspContentChange {
        @JsonProperty public LspRange range;
        @JsonProperty public String text;
    }

    /** LSP Range (start and end positions). */
    static class LspRange {
        @JsonProperty public LspPosition start;
        @JsonProperty public LspPosition end;
    }

    /** LSP CompletionContext. {@code triggerKind}: 1=manual, 2=trigger character, 3=re-trigger. */
    static class LspCompletionContext {
        @JsonProperty public int triggerKind;
        @JsonProperty public String triggerCharacter;
    }
}
