/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.bin.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.ext.r.bin.Activator;
import org.knime.ext.r.bin.RPathUtil;

import com.sun.jna.Platform;

/**
 *
 * @author Heiko Hofer
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RPreferenceInitializer.class);

    /** Preference key for the path to the R executable setting. */
    public static final String PREF_R_HOME = "knime.r.home";

    /** Preference key for for property if a custom setting should be allowed for R nodes which have been deprecated
     * in v2.10.
     */
    public static String PREF_PRE_V2_10_SUPPORT = "knime.r.pre-v2.10-support";

    /** Preference key for Path to R binary. */
    public static final String PREF_R_BIN = "knime.r.bin";

    /**
     * @return default R_HOME from 2.9 settings, package bundle or system default
     */
    private File getDefaultRHOME() {
        // R_HOME value
        String rHome_v29 = org.eclipse.core.runtime.Platform.getPreferencesService().getString(
            "org.knime.r", "knime.r.home", "", null);
        if (rHome_v29 != null && !rHome_v29.isEmpty()) {
            return new File(rHome_v29);
        } else {
            File packagedExecutable = RPathUtil.getPackagedRHome();
            if (packagedExecutable != null) {
                return packagedExecutable;
            }
            return RPathUtil.getSystemRHome();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        File rHomeFile = getDefaultRHOME();
        String rHome = rHomeFile == null ? "" : rHomeFile.getAbsolutePath();
        LOGGER.debug("Default R Home: " + rHome);
        store.setDefault(PREF_R_HOME, rHome);

        // preferences for backward compatibility for KNIME < v2.10
        String rBin_v29 = org.eclipse.core.runtime.Platform.getPreferencesService().getString(
            "org.knime.ext.r", "knime.r.path", "", null);
        boolean enablePreV210Support = rBin_v29 != null && !rBin_v29.isEmpty();

        store.setDefault(PREF_PRE_V2_10_SUPPORT, enablePreV210Support);
        if (enablePreV210Support) {
            store.setDefault(PREF_R_BIN, rBin_v29);
        }
    }

	/**
     * Returns a provider for the R executable.
     * @param plugin the id of the plugin requesting the preference provider.
     *  If null the default preference provider is returned.
     * @return provider to the path to the R executable
     */
    public static final RPreferenceProvider getRProvider(final String plugin) {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        boolean enablePreV210Support = store.getBoolean(PREF_PRE_V2_10_SUPPORT);

        if (enablePreV210Support && plugin != null && plugin.equals("org.knime.ext.r")) {
            final String rBinPath = store.getString(PREF_R_BIN);
            return new RPreferenceProvider() {
                @Override
                /** {@inheritDoc} */
                public String getRHome() {
                    throw new UnsupportedOperationException("Please use getRBinPath() with this provider, only.");
                }

                /** {@inheritDoc} */
                @Override
                public String getRBinPath() {
                    return rBinPath;
                }
            };
        }
        else {
            return new RPreferenceProvider() {
                @Override
                /** {@inheritDoc} */
                public String getRHome() {
                    return Activator.getRHOME().getAbsolutePath();
                }

                /** {@inheritDoc} */
                @Override
                public String getRBinPath() {
                	if (Platform.isWindows()) {
                    	if (Platform.is64Bit()) {
                    		return getRHome() + File.separator + "bin" + File.separator + "x64" + File.separator + "R";
                    	} else {
                    		return getRHome() + File.separator + "bin" + File.separator + "i386" + File.separator + "R";
                    	}
                	} else {
                		return getRHome() + File.separator + "bin" + File.separator + "R";
                	}
                }
            };
        }
    }
}
