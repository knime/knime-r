/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.bin.preferences;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.ext.r.bin.Activator;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.RPathUtil;

/**
 *
 * @author Heiko Hofer
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RPreferenceInitializer.class);

    /** Preference key for the path to the R executable setting. */
    public static final String PREF_R_HOME = "knime.r.home";

    /** From The Rserve docu: maxinbuf specifies the maximal allowable size of the input buffer. */
    // addresses AP-5976
    public static final String PREF_RSERVE_MAXINBUF = "knime.r.maxinbuf";

    /** Default for {@link #PREF_RSERVE_MAXINBUF} in MB. */
    private static final int DEFAULT_MAX_IN_BUF_MB = 256;

    /**
     * @return default R_HOME from 2.9 settings, package bundle or system default
     */
    private File getDefaultRHOME() {
        // R_HOME value from KNIME 2.9
        final String rHomeV29 = org.eclipse.core.runtime.Platform.getPreferencesService().getString("org.knime.r",
            "knime.r.home", "", null);
        if ((rHomeV29 != null) && !rHomeV29.isEmpty()) {
            final File rHomeFile = new File(rHomeV29);
            if (rHomeFile.exists() && rHomeFile.isDirectory()) {
                return rHomeFile;
            }
        }
        // Try R binary settings from KNIME 2.9
        final String rHomeV29FromRBin = determineRHomeFromRBinSetting();
        if ((rHomeV29FromRBin != null) && !rHomeV29FromRBin.isEmpty()) {
            return new File(rHomeV29FromRBin);
        } else {
            final File packagedExecutable = RPathUtil.getPackagedRHome();
            if (packagedExecutable != null) {
                return packagedExecutable;
            }
            return RPathUtil.getSystemRHome();
        }

    }

    private String determineRHomeFromRBinSetting() {
        final String rBinPathV29 = org.eclipse.core.runtime.Platform.getPreferencesService()
            .getString("org.knime.ext.r", "knime.r.path", "", null);
        if ((rBinPathV29 == null) || rBinPathV29.isEmpty()) {
            return null;
        }

        final Properties rProps = RBinUtil.retrieveRProperties(new RPreferenceProvider() {

            @Override
            public String getRHome() {
                throw new UnsupportedOperationException("Please use getRBinPath() with this provider, only.");
            }

            @Override
            public String getRBinPath(final String command) {
                return rBinPathV29;
            }

            @Override
            public int getMaxInfBuf() {
                return DEFAULT_MAX_IN_BUF_MB; // default
            }

            @Override
            public String getRServeBinPath() {
                throw new UnsupportedOperationException("Please use getRBinPath() with this provider, only.");
            }
        });

        if ((rProps != null) && rProps.containsKey("rhome")) {
            final File rhomeFile = new File(rProps.getProperty("rhome"));
            if (rhomeFile.exists()) {
                try {
                    // determine canonical path since R does not provide it.
                    return rhomeFile.getCanonicalPath();
                } catch (final IOException e) {
                    // do nothing
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        final File rHomeFile = getDefaultRHOME();
        final String rHome = rHomeFile == null ? "" : rHomeFile.getAbsolutePath();
        LOGGER.debug("Default R Home: " + rHome);
        store.setDefault(PREF_R_HOME, rHome);
        store.setDefault(PREF_RSERVE_MAXINBUF, DEFAULT_MAX_IN_BUF_MB);
    }

    private static RPreferenceProvider m_cachedPreferenceProvider = null;

    /**
     * Returns a provider for the R executable.
     *
     * @return provider to the path to the R executable
     */
    public static final RPreferenceProvider getRProvider() {
        final String rHome = Activator.getRHOME().getAbsolutePath();
        if ((m_cachedPreferenceProvider == null) || !m_cachedPreferenceProvider.getRHome().equals(rHome)) {
            m_cachedPreferenceProvider = new DefaultRPreferenceProvider(rHome);
        }
        return m_cachedPreferenceProvider;
    }

    /**
     * Invalidate the cached preference provider returned by {@link #getRProvider()}, to refetch R properties (which
     * launches an external R command).
     */
    public static final void invalidatePreferenceProviderCache() {
        m_cachedPreferenceProvider = null;
    }
}
