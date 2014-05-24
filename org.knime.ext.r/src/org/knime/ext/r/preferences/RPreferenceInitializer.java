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
package org.knime.ext.r.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.ext.r.RCorePlugin;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RPreferenceInitializer.class); 

    /** Preference key for the path to the R executable setting. */
    public static final String PREF_R_PATH = "knime.r.path";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = RCorePlugin.getDefault().getPreferenceStore();
        File rPath = RCorePlugin.getRExecutable();
        if (rPath != null) {
            LOGGER.debug("Default R executable: " + rPath.getAbsolutePath());
            store.setDefault(PREF_R_PATH, rPath.getAbsolutePath());
        } else {
            store.setDefault(PREF_R_PATH, "");
        }
    }

    /**
     * Returns a provider for the R executable.
     * @return provider to the path to the R executable
     */
    public static final RPreferenceProvider getRProvider() {
        return new RPreferenceProvider() {
            @Override
            /** {@inheritDoc} */
            public String getRPath() {
                final IPreferenceStore pStore =
                    RCorePlugin.getDefault().getPreferenceStore();
                return pStore.getString(PREF_R_PATH);
            }
        };
    }
}
