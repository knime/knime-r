/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Properties;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.core.node.ExecutionMonitor;
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
public class RBinUtil {
    /**
     * The temp directory used as a working directory for R
     */
    static final String TEMP_PATH = KNIMEConstants.getKNIMETempDir().replace('\\', '/');

    NodeLogger LOGGER = NodeLogger.getLogger(RBinUtil.class);

    private static RBinUtil instance;

    /**
     * Private constructor to prevent instantiation from outside.
     */
    private RBinUtil() {
        // do nothing
    }

    /**
     * Get default instance.
     * @return the default instance
     */
    public static RBinUtil getDefault() {
        if (instance == null) {
            instance = new RBinUtil();
        }
        return instance;
    }

    /**
     * Get properties about the used R.
     * @return properties about use R
     * @throws IOException in case that running R fails
     * @throws InterruptedException when external process of calling R is interrupted
     */
    public Properties retrieveRProperties() throws IOException, InterruptedException {
        return retrieveRProperties(RPreferenceInitializer.getRProvider());
    }

    /**
     * Get properties about the used R.
     * @param rpref provider for path to R executable
     * @return properties about use R
     * @throws IOException in case that running R fails
     * @throws InterruptedException when external process of calling R is interrupted
     */
    public Properties retrieveRProperties(final RPreferenceProvider rpref) throws IOException, InterruptedException {
        final File tmpPath = new File(TEMP_PATH);
        File propsFile = FileUtil.createTempFile("R-propsTempFile-", ".r", true);
        File rOutFile = FileUtil.createTempFile("R-propsTempFile-", ".Rout", tmpPath, true);

        File rCommandFile = writeRcommandFile(
                "setwd(\"" + tmpPath.getAbsolutePath().replace('\\', '/') + "\");\n"
              + "foo <- paste(names(R.Version()), R.Version(), sep=\"=\");\n"
              + "lapply(foo, cat, \"\\n\", file=\"" +  propsFile.getAbsolutePath().replace('\\', '/') + "\", append=TRUE);\n"
              + "foo <- paste(\"memory.limit\", memory.limit(), sep=\"=\");\n"
              + "lapply(foo, cat, \"\\n\", file=\"" +  propsFile.getAbsolutePath().replace('\\', '/') + "\", append=TRUE);\n"
              + "foo <- paste(\"rJava.path\", find.package(\"rJava\", quiet = TRUE), sep=\"=\");\n"
              + "lapply(foo, cat, \"\\n\", file=\"" +  propsFile.getAbsolutePath().replace('\\', '/') + "\", append=TRUE);\n"
              + "foo <- paste(\"rhome\", R.home(), sep=\"=\");\n"
              + "lapply(foo, cat, \"\\n\", file=\"" +  propsFile.getAbsolutePath().replace('\\', '/') + "\", append=TRUE);\n"
              + "");
        // create shell command
        StringBuilder shellCmd = new StringBuilder();
        String rBinaryFile = getRBinaryPathAndArguments(rpref);
        shellCmd.append(rBinaryFile);
        shellCmd.append(" ");
        shellCmd.append(rCommandFile.getName());
        shellCmd.append(" ");
        shellCmd.append(rOutFile.getName());

        CommandExecution cmdExec = new CommandExecution(shellCmd.toString());

        cmdExec.setExecutionDir(rCommandFile.getParentFile());
        try {
            int exitValue = cmdExec.execute(new ExecutionMonitor());
            if (exitValue != 0) {
                StringBuilder stderr = new StringBuilder();
                for (String s : cmdExec.getStdErr()) {
                    stderr.append(s);
                }
                if (stderr.length() > 0) {
                    LOGGER.debug(stderr.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
            return new Properties();
        }

        // read propsFile
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(propsFile);

        // loading properties from properties file
        props.load(fis);

        if (props.size() <= 0) {
            // Something went wrong, report R stdout und stderr.
            // The output and error streams are redirected to *.Rout when executing R with CMD BATCH
            File rOut = new File(rCommandFile.getAbsolutePath() + ".Rout");
            if (rOut.exists() && rOut.isFile()) {
                BufferedReader br = new BufferedReader(new FileReader(rOut));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    LOGGER.debug("Error while investigating the R environment. This is the ouput if R:\n" + sb.toString());
                } catch(InvalidObjectException ioe) {
                    LOGGER.debug("Error when reading file: " + rOut.getAbsolutePath());
                } finally {
                    try {
                        br.close();
                    } catch(InvalidObjectException ioe) {
                        // do nothing
                    }
                }
            }

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
    private File writeRcommandFile(final String cmd) throws IOException {
        File tempCommandFile = FileUtil.createTempFile("R-readPropsTempFile-", ".r", new File(TEMP_PATH), true);
        FileWriter fw = new FileWriter(tempCommandFile);
        fw.write(cmd);
        fw.close();
        return tempCommandFile;
    }

    /**
     * Path to R binary together with the R arguments <code>CMD BATCH</code> and
     * additional options.
     * @return R binary path and arguments
     */
    private final String getRBinaryPathAndArguments(final RPreferenceProvider rpref) {
        String argR = retRArguments();
        if (!argR.isEmpty()) {
            argR = " " + argR;
        }
        return rpref.getRBinPath() + " CMD BATCH" + argR;
    }

    private String retRArguments() {
        return "--vanilla";
    }


    /**
     * Checks whether the given path is a valid R_HOME directory. It checks the presence of the bin and library folder.
     * The methods returns <code>null</code> if the directory is valid, otherwise a detailed error message.
     *
     * @param rHomePath path to R_HOME
     * @return <code>null</code> in case the directory is valid, an error message otherwise
     */
    public String checkRHome(final String rHomePath) {
        File rHome = new File(rHomePath);
        String msgSuffix =
            "R_HOME ('" + rHomePath + "') is meant to be the path to the folder which is the root of Rs"
                + " installation tree. \nIt contains a 'bin' folder which itself contains the R executable and a "
                + "'library' folder containing the R-Java bridge library.\n"
                + "Please change the R settings in the preferences.";
        if (!rHome.exists()) {
            return "R_HOME does not exist. \n" + msgSuffix;
        }
        if (!rHome.isDirectory()) {
            return "R_HOME is not a directory. \n" + msgSuffix;
        }
        File binDir = new File(rHome, "bin");
        if (!binDir.isDirectory()) {
            return "R_HOME does not contain a folder with name 'bin'.\n" + msgSuffix;
        }

        File rExecutable = new File(new DefaultRPreferenceProvider(rHomePath).getRBinPath());
        if (!rExecutable.exists()) {
            return "R_HOME does not contain an R executable.\n" + msgSuffix;
        }

        File libraryDir = new File(rHome, "library");
        if (!libraryDir.isDirectory()) {
            return "R_HOME does not contain a folder with name 'library'.\n" + msgSuffix;
        }
        if (Platform.isWindows()) {
            if (Platform.is64Bit()) {
                File expectedFolder = new File(binDir, "x64");
                if (!expectedFolder.isDirectory()) {
                    return "R_HOME does not contain a folder with name 'bin\\x64'. Please install R 64-bit files.\n"
                        + msgSuffix;
                }
            } else {
                File expectedFolder = new File(binDir, "i386");
                if (!expectedFolder.isDirectory()) {
                    return "R_HOME does not contain a folder with name 'bin\\i386'. Please install R 32-bit files.\n"
                        + msgSuffix;
                }
            }
        }
        return null;
    }
}
