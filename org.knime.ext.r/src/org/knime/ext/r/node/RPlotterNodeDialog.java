/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.r.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.ext.r.node.local.RViewsDialogPanel;
import org.knime.ext.r.node.local.RViewsPngDialogPanel;


/**
 * Dialog of the R plotter to select two numeric columns.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeDialog extends RNodeDialogPane {
    
    private final RViewsDialogPanel m_plotCommandPanel;
    
    private final RViewsPngDialogPanel m_viewPngPanel;
    
    /**
     * New pane for configuring REvaluator node dialog.
     */
    @SuppressWarnings("unchecked")
    protected RPlotterNodeDialog() {
        super();
        m_plotCommandPanel = new RViewsDialogPanel(); 
        super.addTab("R View Command", m_plotCommandPanel);
        m_viewPngPanel = new RViewsPngDialogPanel();
        super.addTab(RViewsPngDialogPanel.TAB_PNG_TITLE, m_viewPngPanel);
        super.addLoginTab();
    }

    /**
     * Calls the update method of the underlying filter panel using the input 
     * data table spec from this <code>FilterColumnNodeModel</code>.
     *  
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException If no columns in spec.
     */
    @Override
    protected void loadSettingsFrom(
            final NodeSettingsRO settings, final DataTableSpec[] specs) 
            throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_plotCommandPanel.loadSettings(settings, specs);
        m_viewPngPanel.loadSettings(settings, specs);
    } 
    
    /**
     * Sets the list of columns to exclude inside the underlying
     * <code>FilterColumnNodeModel</code> retrieving them from the filter panel.
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException If column list does not contain two 
     *         items.
     * @see org.knime.core.node.NodeDialogPane#saveSettingsTo(
     * NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_plotCommandPanel.saveSettings(settings);
        m_viewPngPanel.saveSettings(settings);
    }
}
