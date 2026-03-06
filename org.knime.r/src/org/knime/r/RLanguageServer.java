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
import java.nio.file.Path;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.scripting.lsp.LanguageServerProxy;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;

/**
 * Utility for starting the LSP server for R using the {@code languageserver} package.
 * <p>
 * The server is started via {@code Rscript --no-save --no-restore --slave -e "languageserver::run()"}.
 * The user is responsible for having R and the {@code languageserver} package installed.
 * Configure the R executable path in the KNIME preferences (Preferences → KNIME → R).
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
final class RLanguageServer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RLanguageServer.class);

    private RLanguageServer() {
        // utility class
    }

    /**
     * Start the R language server.
     * <p>
     * Starts {@code Rscript --no-save --no-restore --slave -e "languageserver::run()"} and returns a proxy that
     * forwards LSP messages over the process stdin/stdout.
     *
     * @return a {@link LanguageServerProxy} connected to the running R language server process
     * @throws IOException if the R executable cannot be found or the process cannot be started
     */
    static LanguageServerProxy startLanguageServer() throws IOException {
        final String rscriptPath = RPreferenceInitializer.getRProvider().getRBinPath("Rscript");
        System.out.println("[R LSP] Rscript path from KNIME preferences: " + rscriptPath); // NOSONAR – intentional debug output

        if (rscriptPath == null || rscriptPath.isBlank()) {
            System.out.println("[R LSP] ERROR: No R executable configured in KNIME Preferences → KNIME → R."); // NOSONAR
            throw new IOException(
                "Rscript executable not found. Configure the R installation path in KNIME Preferences → KNIME → R.");
        }

        // Pre-flight: verify that the languageserver package is installed before
        // starting the server process, so we get a clear error message instead of a silent crash.
        System.out.println("[R LSP] Checking that the 'languageserver' package is installed..."); // NOSONAR
        try {
            final Process check = new ProcessBuilder(rscriptPath, "--no-save", "--no-restore", "--slave",
                "-e", "if (!requireNamespace('languageserver', quietly=TRUE)) stop('languageserver not installed')") //
                    .start();
            final String stderr = new String(check.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            final int exit = check.waitFor();
            if (exit != 0) {
                final String msg = "'languageserver' R package is not installed."
                    + " Run install.packages('languageserver') in R to enable autocompletion."
                    + (stderr.isEmpty() ? "" : " (R said: " + stderr + ")");
                System.out.println("[R LSP] " + msg); // NOSONAR
                throw new IOException(msg);
            }
            System.out.println("[R LSP] 'languageserver' package is available."); // NOSONAR
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking 'languageserver' package", e);
        }

        // Diagnostics (lintr) are disabled because the scripting editor uses a virtual
        // inmemory:// document URI, and lintr's normalizePath() fails on non-file URIs.
        // Hover, completions and signature-help are unaffected by this option.
        final String rExpr = "options(languageserver.diagnostics = FALSE); languageserver::run()";
        System.out.println("[R LSP] Starting: " + rscriptPath // NOSONAR
            + " --no-save --no-restore --slave -e \"" + rExpr + "\"");
        LOGGER.debug("Starting R language server using: " + rscriptPath);

        final ProcessBuilder pb = new ProcessBuilder(rscriptPath, "--no-save", "--no-restore", "--slave",
            "-e", rExpr);

        // Set working directory to the user's home so that callr (used internally by languageserver
        // for completion and parsing) can create temp files and spawn subprocesses successfully.
        pb.directory(Path.of(System.getProperty("user.home")).toFile());

        // Ensure R's bin directory is on PATH so that callr/processx can find R.exe when
        // spawning background subprocesses for completions. KNIME's JVM typically does not
        // include R in its PATH, which causes processx to fail to start the R subprocess.
        final String rBinDir = Path.of(rscriptPath).getParent().toString();
        final var env = pb.environment();
        final String pathKey = env.containsKey("PATH") ? "PATH" : "Path";
        final String existingPath = env.getOrDefault(pathKey, "");
        if (!existingPath.contains(rBinDir)) {
            env.put(pathKey, rBinDir + File.pathSeparator + existingPath);
        }

        // Redirect R's stderr to the Eclipse Console so crash output is immediately visible.
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        final LanguageServerProxy proxy = new LanguageServerProxy(pb);
        System.out.println("[R LSP] LanguageServerProxy created — LSP process is running."); // NOSONAR
        return proxy;
    }
}
