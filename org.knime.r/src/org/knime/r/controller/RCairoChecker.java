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
 *   Jan 30, 2020 (benjamin): created
 */
package org.knime.r.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.NodeLogger;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
import org.knime.r.controller.IRController.RException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

/**
 * Utility class to check if cairo is available.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
final class RCairoChecker {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RCairoChecker.class);

    private RCairoChecker() {
        // Utility class
    }

    /** The Cairo lib can be installed in different environments */
    private static Set<RPreferenceProvider> cairoFound = new HashSet<>();

    /** Quarz is installed on the system */
    private static boolean quartzFound = false;

    /**
     * Check if Cairo is available. Uses cached results for the given preferences.
     *
     * @param preferences the current R preferences
     * @param controller the R controller to run R commands
     * @throws RException if Cairo is present but XQuarz is not installed
     */
    static void checkCairoOnMac(final RPreferenceProvider preferences, final RController controller) throws RException {
        if (cairoFound.contains(preferences) && quartzFound) {
            return;
        }

        // produce a warning message if 'Cairo' package is not installed.
        try {
            final REXP ret = controller.eval("find.package('Cairo')", true);
            final String cairoPath = ret.asString();

            if (!StringUtils.isEmpty(cairoPath)) {
                // under Mac we need Cairo package to use png()/bmp() etc devices.
                cairoFound.add(preferences);
            }
        } catch (RException | REXPMismatchException e) {
            LOGGER.debug("Error while querying Cairo package version: " + e.getMessage(), e);
        }

        if (!cairoFound.contains(preferences)) {
            LOGGER.warn("The package 'Cairo' needs to be installed in your R installation for bitmap graphics "
                + "devices to work properly. Please install it in R using \"install.packages('Cairo')\".");
            return;
        }

        // Cairo requires XQuartz to be installed. We make sure it is, since
        // loading the Cairo library will crash Rserve otherwise.
        if (quartzFound) {
            // We already checked it for another environment
            return;
        }
        final ProcessBuilder builder =
            new ProcessBuilder("mdls", "-name", "kMDItemVersion", "/Applications/Utilities/XQuartz.app");

        try {
            final Process process = builder.start();

            // check if output of process was a valid version
            final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdout.readLine()) != null) {
                if (line.matches("kMDItemVersion = \"2(?:\\.[0-9]+)+\"")) {
                    quartzFound = true;
                }
            }

            try {
                process.waitFor();
            } catch (final InterruptedException e) {
                // happens when user cancels node at this point for example
                LOGGER.debug("Interrupted while waiting for mdls process to terminate.", e);
            }
        } catch (final IOException e) {
            // should never happen, just in case, here is something for
            // users to report if they accidentally deleted their mdls
            LOGGER.error("Could not run mdls to check for XQuartz version: " + e.getMessage(), e);
        }

        if (!quartzFound) {
            throw new RException("XQuartz is required for the Cairo library on MacOS. Please download "
                + "and install XQuartz from http://www.xquartz.org/.", null);
        }
    }
}
