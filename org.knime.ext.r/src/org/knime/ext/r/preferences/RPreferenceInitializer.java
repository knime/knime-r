/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the R integration plugin for KNIME.
 *
 * The R integration plugin is free software; you can redistribute 
 * it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.org.
 * ---------------------------------------------------------------------
 * 
 * History
 *   19.09.2007 (thiel): created
 */
package org.knime.ext.r.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.ext.r.RCorePlugin;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RPreferenceInitializer extends AbstractPreferenceInitializer {

    /** Preference key for the path to the R executable setting. */
    public static final String PREF_R_PATH = "knime.r.path";    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = RCorePlugin.getDefault().getPreferenceStore();

        //set default values
        store.setDefault(PREF_R_PATH, "");
    }

    /**
     * Returns the path to the R executable.
     * 
     * @return the path
     */
    public static String getRPath() {
        final IPreferenceStore pStore = 
            RCorePlugin.getDefault().getPreferenceStore();
        return pStore.getString(PREF_R_PATH);
    }
}
