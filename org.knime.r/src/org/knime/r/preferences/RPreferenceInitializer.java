/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.r.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.r.Activator;

/**
 *
 * @author Heiko Hofer
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RPreferenceInitializer.class); 

    /** Preference key for the path to the R executable setting. */
    public static final String PREF_R_HOME = "knime.r.home";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String rHome = getDefaultRHome();
        rHome = rHome == null ? "" : rHome;
        LOGGER.info("Default R Home: " + rHome);
        store.setDefault(PREF_R_HOME, rHome);
    }

    private String getDefaultRHome() {
		return "/usr/lib/R";
	}

	/**
     * Returns a provider for the R executable.
     * @return provider to the path to the R executable
     */
    public static final RPreferenceProvider getRProvider() {
        return new RPreferenceProvider() {
            @Override
            /** {@inheritDoc} */
            public String getRHome() {
                final IPreferenceStore pStore =
                    Activator.getDefault().getPreferenceStore();
                return pStore.getString(PREF_R_HOME);
            }
        };
    }
}
