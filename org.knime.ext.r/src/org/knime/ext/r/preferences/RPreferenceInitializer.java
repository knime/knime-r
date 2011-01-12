/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as 
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
            LOGGER.info("Default R executable: " + rPath.getAbsolutePath());
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
