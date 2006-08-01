/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
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
package de.unikn.knime.r.node;

import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

import org.knime.base.node.util.FilterColumnPanel;

/**
 * Dialog of the R plotter to select two numeric columns.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeDialog extends RNodeDialogPane {
    private final FilterColumnPanel m_dialogPanel;
    
    /**
     * New pane for configuring REvaluator node dialog.
     */
    protected RPlotterNodeDialog() {
        super();
        m_dialogPanel = new FilterColumnPanel(DoubleValue.class);
        super.addTab("Options", m_dialogPanel);
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
        String[] columns = settings.getStringArray(
                "PLOT", new String[0]); 
        m_dialogPanel.update(specs[0], false, columns);
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
        Set<String> list = m_dialogPanel.getIncludedColumnList();
        if (list.size() != 2) {
            throw new InvalidSettingsException("Select two columns.");
        }
        settings.addStringArray("PLOT", list.toArray(new String[0]));
    }
}
