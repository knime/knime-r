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
 *   17.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.ext.r.node.RDialogPanel;


/**
 * A dialog containing a multi line text field to specify the R code to run.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalLearnerNodeDialogPane extends RLocalNodeDialogPane {

    private final RDialogPanel m_dialogPanel;
    
    private static final String TAB_R_BINARY = "R Binary";
    
    /**
     * Constructor which creates a new instance of 
     * <code>RLocalScriptingNodeDialogPane</code>. A dialog component is added
     * which allows users to enter R code. 
     */
    public RLocalLearnerNodeDialogPane() {
        super();
        m_dialogPanel = new RDialogPanel();
        
        addTab("R command", m_dialogPanel);
        setDefaultTabTitle(TAB_R_BINARY);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        m_dialogPanel.loadSettingsFrom(settings, specs);
    } 
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        m_dialogPanel.saveSettingsTo(settings);
    }    
}
