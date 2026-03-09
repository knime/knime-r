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

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.knime.core.util.ThreadUtils;
import org.knime.core.webui.node.dialog.scripting.ScriptingService;
import org.knime.core.webui.node.dialog.scripting.lsp.LanguageServerProxy;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.r.controller.ConsoleLikeRExecutor;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link ScriptingService} implementation for the interactive R scripting dialog. Manages a live R session via
 * {@link RController} and exposes RPC methods for running scripts, resetting the workspace, and killing the session.
 *
 * <h2>LSP Compatibility Layer</h2>
 * <p>
 * The R {@code languageserver} package (version 0.3.16) has compatibility issues with the Monaco-based
 * LSP client used by {@code @knime/scripting-editor}:
 *
 * <h3>Problem 1: Full vs. Incremental Text Synchronization Mismatch</h3>
 * <ul>
 *   <li><b>Root cause:</b> R {@code languageserver} advertises {@code "textDocumentSync":{"change":1}}
 *       (Full sync) in its {@code initialize} response, indicating it expects the complete document
 *       content in every {@code textDocument/didChange} notification.</li>
 *   <li><b>Client behavior:</b> {@code MonacoLSPConnection} always sends <em>incremental</em> changes
 *       (range-based deltas) regardless of the server's advertised capability.</li>
 *   <li><b>Server bug:</b> The R {@code text_document_did_change} handler reads only
 *       {@code contentChanges[[1]]$text} and ignores the {@code range} field, treating the delta text
 *       as the new full document. For example, typing a newline sends {@code {range: {…}, text: "\n"}},
 *       which R interprets as "replace the entire document with a single newline".</li>
 *   <li><b>Symptom:</b> After the first keystroke, hover and completion return empty results because
 *       R's internal document state is corrupted.</li>
 *   <li><b>Fix:</b> Maintain a Java-side document replica. On each {@code didChange}, apply incremental
 *       deltas to the replica, then send R a full-document update (no {@code range} field).</li>
 * </ul>
 *
 * <h3>Problem 2: Empty-Line Completion Guard</h3>
 * <ul>
 *   <li><b>Root cause:</b> R {@code languageserver}'s {@code completion_reply} has an
 *       {@code if (nzchar(full_token))} guard that skips all completion providers when the token
 *       at the cursor is empty.</li>
 *   <li><b>Symptom:</b> Pressing Ctrl+Space on an empty line returns {@code {"items":[]}} — no
 *       completions for KNIME objects like {@code knime.in}, {@code knime.out}.</li>
 *   <li><b>Fix:</b> When a manual completion ({@code triggerKind:1}) arrives at {@code character:0}
 *       on an empty line, temporarily inject the letter "k" at the cursor position, send the
 *       completion request at {@code character:1}, then immediately restore the original document.
 *       R processes messages sequentially, so this triplet is atomic.</li>
 * </ul>
 *
 * <h3>Problem 3: CRLF Line Endings from Windows WebUI</h3>
 * <ul>
 *   <li><b>Root cause:</b> On Windows, the browser-based scripting editor (CodeMirror/Monaco) may
 *       send {@code \r\n} (CRLF) line endings when the user types or pastes text.</li>
 *   <li><b>Server bug:</b> R's parser treats {@code \r} as an unexpected character, producing errors
 *       like "unexpected input" at the position right after the last character of a line (the hidden
 *       {@code \r}).</li>
 *   <li><b>Fix:</b> Normalize all incoming text to LF-only ({@code \n}) in {@code didOpen} and
 *       {@code didChange} before storing in the document replica or sending to R.</li>
 * </ul>
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
class RScriptingService extends ScriptingService {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RScriptingService.class);

    /**
     * Shared Jackson ObjectMapper for parsing and generating LSP messages.
     * Configured to omit null fields for cleaner JSON output.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
     * The JSON-RPC service exposed to the frontend. Implements the LSP message translation layer
     * described in the class-level documentation to work around R {@code languageserver} bugs.
     *
     * <p>Must be public for the JSON-RPC server to access it.
     */
    public final class RRpcService extends RpcService {

        /**
         * URI of the currently open document, captured from {@code textDocument/didOpen}.
         * Used when constructing synthetic {@code didChange} messages for the empty-line completion workaround.
         */
        private String m_lspDocumentUri = null;

        /**
         * Java-side replica of the document content (LF line endings only).
         * <p>
         * Initialized from {@code textDocument/didOpen} and kept in sync by applying incremental
         * deltas from each {@code textDocument/didChange}. We maintain this because:
         * <ul>
         *   <li>R {@code languageserver} cannot handle incremental (range-based) changes correctly —
         *       it treats the delta text as the new full document.</li>
         *   <li>We normalize CRLF→LF: Windows browsers may send {@code \r\n}, which R's parser
         *       rejects as "unexpected input".</li>
         * </ul>
         */
        private String m_lspDocumentContent = "";

        /**
         * Version number for {@code textDocument/didChange} notifications.
         * <p>
         * LSP requires monotonically increasing version numbers. We track the last version sent to
         * R so that synthetic messages (empty-line completion workaround) can use correct version
         * numbers (previous + 1, previous + 2, etc.). This ensures R's internal version tracking
         * stays consistent.
         */
        private int m_lspVersion = 0;

        /**
         * Intercepts outgoing LSP messages and applies necessary transformations for R {@code languageserver}
         * compatibility.
         *
         * <p><b>What this does:</b></p>
         * <ul>
         *   <li>{@code textDocument/didOpen}: Normalize CRLF→LF, initialize document replica.</li>
         *   <li>{@code textDocument/didChange}: Apply incremental deltas to replica, send full-document update to R.</li>
         *   <li>{@code textDocument/completion} at empty line: Inject temporary "k" prefix to work around
         *       R's {@code nzchar(full_token)} guard.</li>
         *   <li>All other messages: Forward unchanged.</li>
         * </ul>
         *
         * <p><b>Why this is necessary:</b> See class-level documentation for detailed problem descriptions.</p>
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
         * Processes {@code textDocument/didOpen}: initializes the document replica and normalizes line endings.
         *
         * <p><b>Implementation:</b></p>
         * <ol>
         *   <li>Parse the incoming message using Jackson.</li>
         *   <li>Normalize CRLF→LF in the document text (Windows browsers may send {@code \r\n}).</li>
         *   <li>Store the normalized text in {@link #m_lspDocumentContent} and URI in {@link #m_lspDocumentUri}.</li>
         *   <li>Rebuild the message with the normalized text and forward to R.</li>
         * </ol>
         *
         * <p><b>Why normalize line endings:</b> R's parser treats {@code \r} as an unexpected character,
         * producing "unexpected input" errors. The legacy Swing-based dialog used {@code RSyntaxDocument},
         * which normalizes line endings automatically. We must do the same for the WebUI.</p>
         *
         * @param message the original {@code textDocument/didOpen} message from the frontend
         * @return the message with normalized text, or the original message if parsing fails
         */
        private String processDidOpen(final String message) {
            try {
                final JsonNode root = MAPPER.readTree(message);
                final JsonNode params = root.get("params");
                if (params == null) {
                    LOGGER.warn("[R-LSP] didOpen: no 'params' field – forwarding unchanged");
                    return message;
                }

                final LspDidOpenParams didOpen = MAPPER.treeToValue(params, LspDidOpenParams.class);
                if (didOpen.textDocument == null || didOpen.textDocument.text == null) {
                    LOGGER.warn("[R-LSP] didOpen: missing textDocument/text – forwarding unchanged");
                    return message;
                }

                // Normalize CRLF→LF and store document
                m_lspDocumentContent = didOpen.textDocument.text
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");
                m_lspDocumentUri = didOpen.textDocument.uri;

                // Rebuild message with normalized text
                didOpen.textDocument.text = m_lspDocumentContent;
                final JsonNode updatedParams = MAPPER.valueToTree(didOpen);
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params", updatedParams);
                return MAPPER.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] didOpen: JSON parsing failed – forwarding unchanged", e);
                return message;
            }
        }

        /**
         * Handles a {@code textDocument/didChange} message: applies each incremental content change to
         * {@link #m_lspDocumentContent} and rebuilds the message with a single full-document
         * {@code contentChanges} entry (no {@code range}), as required by R {@code languageserver}.
         */
        private String processDidChange(final String message) {
            try {
                final JsonNode root = MAPPER.readTree(message);
                final JsonNode params = root.get("params");
                if (params == null) {
                    LOGGER.warn("[R-LSP] didChange: no 'params' field – forwarding unchanged");
                    return message;
                }

                final LspDidChangeParams didChange = MAPPER.treeToValue(params, LspDidChangeParams.class);
                if (didChange.contentChanges == null || didChange.contentChanges.isEmpty()) {
                    LOGGER.warn("[R-LSP] didChange: no contentChanges – forwarding unchanged");
                    return message;
                }

                // Track version for synthetic messages
                if (didChange.textDocument != null && didChange.textDocument.version > m_lspVersion) {
                    m_lspVersion = didChange.textDocument.version;
                }

                // Apply each incremental change to our document replica
                for (final LspContentChange change : didChange.contentChanges) {
                    final String changeText = change.text != null
                        ? change.text.replace("\r\n", "\n").replace("\r", "\n")
                        : "";

                    if (change.range != null) {
                        // Incremental change - apply range
                 m_lspDocumentContent = applyIncrementalChange(m_lspDocumentContent, change.range,
                            changeText);
                    } else {
                        // Full document replacement
                        m_lspDocumentContent = changeText;
                    }
                }

                // Rebuild as full-document change (no range) for R languageserver
                final LspDidChangeParams fullDocChange = new LspDidChangeParams();
                fullDocChange.textDocument = didChange.textDocument;
                fullDocChange.contentChanges = List.of(createFullDocContentChange(m_lspDocumentContent));

                final JsonNode updatedParams = MAPPER.valueToTree(fullDocChange);
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params", updatedParams);
                return MAPPER.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] didChange: JSON parsing failed – forwarding unchanged", e);
                return message;
            }
        }

        /**
         * Applies an incremental LSP text change to the document.
         *
         * <p><b>Implementation:</b> Convert line/character positions to string offsets, then perform
         * substring replacement. LSP positions are 0-based and character offsets are measured in
         * UTF-16 code units (which equals Java {@code char} count for BMP characters).</p>
         *
         * <p><b>Edge cases:</b> Clamps positions to document bounds and validates start ≤ end to
         * handle malformed ranges gracefully.</p>
         *
         * @param document current document content (LF line endings only)
         * @param range the LSP range to replace (start and end positions)
         * @param newText the replacement text (LF line endings)
         * @return the modified document
         */
        private String applyIncrementalChange(final String document, final LspRange range, final String newText) {
            final String[] lines = document.split("\n", -1);

            // Calculate start offset
            int startOffset = 0;
            for (int i = 0; i < Math.min(range.start.line, lines.length); i++) {
                startOffset += lines[i].length() + 1; // +1 for \n
            }
            if (range.start.line < lines.length) {
                startOffset += Math.min(range.start.character, lines[range.start.line].length());
            }

            // Calculate end offset
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

        /**
         * Creates a full-document content change (no range).
         */
        private LspContentChange createFullDocContentChange(final String text) {
            final LspContentChange change = new LspContentChange();
            change.text = text;
            change.range = null;
            return change;
        }

        /**
         * Processes {@code textDocument/completion} requests, with a workaround for empty-line completions.
         *
         * <p><b>Problem:</b> R {@code languageserver}'s {@code completion_reply} function has an
         * {@code if (nzchar(full_token))} guard that skips all completion providers when the token at
         * the cursor is empty. On an empty line at {@code character:0}, the token is {@code ""},
         * so R returns {@code {"items":[]}} — the user sees nothing when pressing Ctrl+Space on a blank line.</p>
         *
         * <p><b>Root cause in R source:</b>
         * <pre>
         * # languageserver/R/handlers-completion.r (line ~130)
         * completion_reply &lt;- function(...) {
         *   token_result &lt;- document$detect_token(point, forward = FALSE)
         *   full_token &lt;- token_result$full_token
         *   ...
         *   if (nzchar(full_token)) {   # ← guard: only runs when token is non-empty!
         *     completions &lt;- c(completions, constant_completion(token),
         *                      package_completion(token), scope_completion(...))
         *   }
         *   ...
         * }
         * </pre>
         *
         * <p><b>Solution:</b> When a manual completion ({@code triggerKind:1}) arrives at
         * {@code character:0} on an empty line, we temporarily inject the letter {@code "k"} at the
         * cursor position, send the patched document to R, forward the completion request at
         * {@code character:1}, then immediately restore the original document. Because R processes
         * LSP messages sequentially, this triplet (patch → completion → restore) is atomic.</p>
         *
         * <p><b>Why "k":</b> KNIME objects ({@code knime.in}, {@code knime.out}, {@code knime.flow.in})
         * all start with "k", so R's completion providers return them when the prefix is "k".</p>
         *
         * <p><b>Implementation:</b></p>
         * <ol>
         *   <li>Parse the completion request.</li>
         *   <li>If not (manual trigger at character:0 on empty line), forward unchanged.</li>
         *   <li>Build patched document: insert "k" at the start of the empty line.</li>
         *   <li>Send {@code didChange} with patched document (version = {@code m_lspVersion + 1}).</li>
         *   <li>Forward completion request with {@code character:1} (cursor after "k").</li>
         *   <li>Send {@code didChange} with original document (version = {@code m_lspVersion + 2}).</li>
         * </ol>
         *
         * @param message the original {@code textDocument/completion} request from the frontend
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
                
                // Only intercept manual Ctrl+Space (triggerKind:1) at character:0 on an empty line
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

                // Build patched content with "k" at the start of the empty line
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

                // Step 1: Send patched document with "k"
                sendDidChange(uri, ++m_lspVersion, patchedContent);

                // Step 2: Forward completion at character:1
                completion.position.character = 1;
                final JsonNode updatedParams = MAPPER.valueToTree(completion);
                ((com.fasterxml.jackson.databind.node.ObjectNode)root).set("params", updatedParams);
                super.sendLanguageServerMessage(MAPPER.writeValueAsString(root));

                // Step 3: Restore original document
                sendDidChange(uri, ++m_lspVersion, m_lspDocumentContent);
            } catch (JsonProcessingException e) {
                LOGGER.warn("[R-LSP] completion: JSON parsing failed – forwarding unchanged", e);
                super.sendLanguageServerMessage(message);
            }
        }

        /**
         * Sends a {@code textDocument/didChange} notification with the full document content.
         *
         * <p><b>DRY principle:</b> This helper eliminates duplicated message construction. It is used by:
         * <ul>
         *   <li>{@link #processDidChange} — normal incremental→full conversion</li>
         *   <li>{@link #handleCompletion} — temporary document patching for the empty-line workaround</li>
         * </ul>
         *
         * @param uri the document URI (typically {@code "file://script.R"})
         * @param version the LSP version number from the original request
         * @param content the complete document text to send (must use LF line endings)
         */
        private void sendDidChange(final String uri, final int version, final String content) {
            try {
                final LspDidChangeParams didChange = new LspDidChangeParams();
                didChange.textDocument = new LspVersionedTextDocumentIdentifier();
                didChange.textDocument.uri = uri;
                didChange.textDocument.version = version;
                didChange.contentChanges = List.of(createFullDocContentChange(content));

                final JsonNode root = MAPPER.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("method", "textDocument/didChange")
                    .set("params", MAPPER.valueToTree(didChange));
                super.sendLanguageServerMessage(MAPPER.writeValueAsString(root));
            } catch (JsonProcessingException e) {
                LOGGER.error("[R-LSP] Failed to send didChange", e);
            }
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

        @Override
        protected org.knime.core.webui.node.dialog.scripting.CodeGenerationRequest getCodeSuggestionRequest(
            final String userPrompt, final String currentCode,
            final org.knime.core.webui.node.dialog.scripting.InputOutputModel[] inputOutputModels) {
            throw new UnsupportedOperationException("Code generation is not yet supported for R nodes.");
        }
    }

    // -----------------------------------------------------------------------
    // LSP Message POJOs (for Jackson serialization/deserialization)
    // -----------------------------------------------------------------------
    //
    // These classes mirror the LSP protocol message structures. We use Jackson instead of manual
    // JSON string manipulation for:
    //   1. Correctness: Proper JSON escaping, Unicode handling, nested objects
    //   2. Maintainability: Changes to message structure only require updating POJOs
    //   3. Readability: Intent is clear (e.g., "didChange.textDocument.version" vs. parsing offsets)
    //
    // All fields are public with @JsonProperty for Jackson to access them (no getters/setters needed).
    //
    // LSP spec: https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/
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

    /**
     * LSP ContentChange.
     * <p>
     * If {@code range} is {@code null}, the change replaces the entire document.
     * If {@code range} is present, the change replaces the specified range with {@code text}.
     */
    static class LspContentChange {
        @JsonProperty public LspRange range; // null for full-document changes
        @JsonProperty public String text;
    }

    /** LSP Range (start and end positions). */
    static class LspRange {
        @JsonProperty public LspPosition start;
        @JsonProperty public LspPosition end;
    }

    /**
     * LSP CompletionContext.
     * <p>
     * {@code triggerKind}: 1 = manual (Ctrl+Space), 2 = trigger character, 3 = re-trigger
     */
    static class LspCompletionContext {
        @JsonProperty public int triggerKind;
        @JsonProperty public String triggerCharacter;
    }
}

