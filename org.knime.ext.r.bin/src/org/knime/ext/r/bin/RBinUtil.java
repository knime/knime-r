/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   13.06.2014 (hofer): created
 */
package org.knime.ext.r.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.bin.preferences.DefaultRPreferenceProvider;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;

import com.sun.jna.Platform;

/**
 * Utility class with methods to call R binary.
 *
 * @author Heiko Hofer
 */
public final class RBinUtil {

    private RBinUtil() {}

    /**
     * The temp directory used as a working directory for R.
     */
    static final String TEMP_PATH = KNIMEConstants.getKNIMETempDir().replace('\\', '/');

    static NodeLogger LOGGER = NodeLogger.getLogger(RBinUtil.class);

    /**
     * Exception thrown when the specified R_HOME directory is invalid.
     *
     * @author Jonathan Hale
     */
    public static class InvalidRHomeException extends Exception {

        /** Generated serialVersionUID */
        private static final long serialVersionUID = -4082365839749450179L;

        /**
         * Constructor.
         *
         * @param msg error message
         */
        public InvalidRHomeException(final String msg) {
            super(msg);
        }

        /**
         * Constructor.
         *
         * @param msg error message
         * @param cause Throwable which caused this exception
         */
        public InvalidRHomeException(final String msg, final Throwable cause) {
            super(cause);
        }
    }

    /**
     * Get properties about the used R.
     *
     * @return properties about use R
     * @throws IOException in case that running R fails
     */
    public static Properties retrieveRProperties() throws IOException {
        return retrieveRProperties(RPreferenceInitializer.getRProvider());
    }

    /**
     * Get properties about the used R installation.
     *
     * @param rpref provider for path to R executable
     * @return properties about use R
     */
    public static Properties retrieveRProperties(final RPreferenceProvider rpref) {
        return retrieveRProperties(rpref.getRBinPath("Rscript"));
    }

    /**
     * Get properties about the used R installation.
     *
     * @param pathToRScriptExecutable Path to Rscript executable
     * @return properties about use R
     */
    public static Properties retrieveRProperties(final String pathToRScriptExecutable) {
        final File tmpPath = new File(TEMP_PATH);
        File propsFile = null;
        File rOutFile = null;
        try {
            propsFile = FileUtil.createTempFile("R-propsTempFile-", ".r", true);
            rOutFile = FileUtil.createTempFile("R-propsTempFile-", ".Rout", tmpPath, true);
        } catch (final IOException e) {
            LOGGER.error("Could not create temporary files for R execution.", e);
            return new Properties();
        }

        final String propertiesPath = propsFile.getAbsolutePath().replace('\\', '/');
        final String script = "setwd('" + tmpPath.getAbsolutePath().replace('\\', '/') + "')\n"
            + "foo <- paste(names(R.Version()), R.Version(), sep='=')\n"//
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "foo <- paste('memory.limit', memory.limit(), sep='=')\n"//
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "foo <- paste('Rserve.path', find.package('Rserve', quiet=TRUE), sep='=')\n"//
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "foo <- paste('Cairo.path', find.package('Cairo', quiet=TRUE), sep='=')\n"//
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "foo <- paste('rhome', R.home(), sep='=')\n" //
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "foo <- paste('Rserve.version', packageVersion('Rserve'), sep='=')\n" //
            + "write(foo, file='" + propertiesPath + "', ncolumns=1, append=TRUE, sep='\\n')\n"//
            + "q()";

        File rCommandFile = null;
        try {
            rCommandFile = writeRcommandFile(script);
        } catch (final IOException e) {
            LOGGER.error("Could not write R command file.", e);
            return new Properties();
        }
        final ProcessBuilder builder = new ProcessBuilder();
        builder.command(pathToRScriptExecutable, "--vanilla", rCommandFile.getName(), rOutFile.getName());
        builder.directory(rCommandFile.getParentFile());

        /* Run R on the script to get properties */
        Process process = null;

        try {
            process = builder.start();
            try (final InputStream inputStream = process.getInputStream();
                    final InputStream errorStream = process.getErrorStream()) {
                final BufferedReader outputReader = new BufferedReader(new InputStreamReader(inputStream));
                final BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

                // Consume the output produced by the R process, otherwise may block process on some operating systems
                new Thread(() -> {
                    try {
                        final StringBuilder b = new StringBuilder();
                        String line;
                        while ((line = outputReader.readLine()) != null) {
                            b.append(line);
                        }
                        LOGGER.debug("External Rscript process output: " + b.toString());
                    } catch (final IOException e) {
                        LOGGER.error("Error reading output of external R process.", e);
                    }
                }, "R Output Reader").start();
                new Thread(() -> {
                    try {
                        final StringBuilder b = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            b.append(line);
                        }
                        LOGGER.debug("External Rscript process error output: " + b.toString());
                    } catch (final IOException e) {
                        LOGGER.error("Error reading error output of external R process.", e);
                    }
                }, "R Error Reader").start();

                process.waitFor();
            }
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            return new Properties();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }

        // load properties from propsFile
        final Properties props = new Properties();
        try(final FileInputStream is = new FileInputStream(propsFile)) {
            props.load(is);
        } catch (final IOException e) {
            LOGGER.warn("Could not retrieve properties from R.", e);
        }

        return props;
    }

    /**
     * Writes the given string into a file and returns it.
     *
     * @param cmd The string to write into a file.
     * @return The file containing the given string.
     * @throws IOException If string could not be written to a file.
     */
    private static File writeRcommandFile(final String cmd) throws IOException {
        final File tempCommandFile = FileUtil.createTempFile("R-readPropsTempFile-", ".r", new File(TEMP_PATH), true);
        try (final FileWriter fw = new FileWriter(tempCommandFile)) {
            fw.write(cmd);
        }
        return tempCommandFile;
    }

    /**
     * @param rHomePath
     * @throws InvalidRHomeException
     */
    public static void checkRHome(final String rHomePath) throws InvalidRHomeException {
        checkRHome(rHomePath, false);
    }

    /**
     * Checks whether the given path is a valid R_HOME directory. It checks the presence of the bin and library folder.
     *
     * @param rHomePath path to R_HOME
     * @param fromPreferences Set to true if this function is called from the R preference page.
     * @throws InvalidRHomeException If the specified R_HOME path is invalid
     */
    public static void checkRHome(final String rHomePath, final boolean fromPreferences) throws InvalidRHomeException {
        final File rHome = new File(rHomePath);
        final String msgSuffix = ((fromPreferences) ? ""
            : " R_HOME ('" + rHomePath + "')" + " is meant to be the path to the folder which is the root of R's "
                + "installation tree. \nIt contains a 'bin' folder which itself contains the R executable and a "
                + "'library' folder. Please change the R settings in the preferences.");
        final String R_HOME_NAME = (fromPreferences) ? "Path to R Home" : "R_HOME";

        /* check if the directory exists */
        if (!rHome.exists()) {
            throw new InvalidRHomeException(R_HOME_NAME + " does not exist." + msgSuffix);
        }
        /* Make sure R home is not a file. */
        if (!rHome.isDirectory()) {
            throw new InvalidRHomeException(R_HOME_NAME + " is not a directory." + msgSuffix);
        }
        /* Check if there is a bin directory */
        final File binDir = new File(rHome, "bin");
        if (!binDir.isDirectory()) {
            throw new InvalidRHomeException(R_HOME_NAME + " does not contain a folder with name 'bin'." + msgSuffix);
        }
        /* Check if there is an R Excecutable */
        final File rExecutable = new File(new DefaultRPreferenceProvider(rHomePath).getRBinPath("R"));
        if (!rExecutable.exists()) {
            throw new InvalidRHomeException(R_HOME_NAME + " does not contain an R executable." + msgSuffix);
        }
        /* Make sure there is a library directory */
        final File libraryDir = new File(rHome, "library");
        if (!libraryDir.isDirectory()) {
            throw new InvalidRHomeException(
                R_HOME_NAME + " does not contain a folder with name 'library'." + msgSuffix);
        }
        /* On windows, we expect the appropriate platform-specific folders corresponding to our Platform */
        if (Platform.isWindows()) {
            final int bits = Platform.is64Bit() ? 64 : 32;
            final String folderName = Platform.is64Bit() ? "x64" : "i386";

            final File expectedFolder = new File(binDir, folderName);
            if (!expectedFolder.isDirectory()) {
                throw new InvalidRHomeException(
                    R_HOME_NAME + " does not contain a folder with name 'bin\\" + folderName + "'. Please install R " + bits + "-bit files."
                        + msgSuffix);
            }
        }
    }
}
