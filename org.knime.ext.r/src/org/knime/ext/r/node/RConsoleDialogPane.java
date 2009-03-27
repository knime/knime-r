/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.r.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * <code>NodeDialogPane</code> for R Console.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RConsoleDialogPane extends RNodeDialogPane {
    private final RDialogPanel m_dialogPanel;

    /**
     * New pane for configuring REvaluator node dialog.
     */
    protected RConsoleDialogPane() {
        super();
        m_dialogPanel = new RDialogPanel() {
            /**
             * Replaces illegal characters in the specified string. Legal
             * characters are a-z, A-Z and 0-9. All others will be replaced by
             * an underscore.
             * 
             * @param name the string to check and to replace illegal characters
             *            in.
             * @return a string containing only a-z, A-Z, 0-9 and _. All other
             *         characters got replaced by an underscore ('_').
             */
            @Override
            protected String formatColumn(final String name) {
                return RConnectionRemote.formatColumn(name);
            }
        };
        this.addTab("R Command", m_dialogPanel);
        super.addLoginTab();
    }

    /**
     * Calls the update method of the underlying filter panel using the input 
     * data table spec from this <code>FilterColumnNodeModel</code>.
     *  
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException If not configurable.
     */
    @Override
    protected void loadSettingsFrom(
            final NodeSettingsRO settings, final DataTableSpec[] specs) 
            throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_dialogPanel.loadSettingsFrom(settings, specs);
    } 
    
    /**
     * Sets the list of columns to exclude inside the underlying
     * <code>FilterColumnNodeModel</code> retrieving them from the filter panel.
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException If settings wrong.
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_dialogPanel.saveSettingsTo(settings);
    }
}
