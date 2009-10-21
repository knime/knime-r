/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
package org.knime.ext.r.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog pane holding the default login panel.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class RNodeDialogPane extends NodeDialogPane {
    
    private final RLoginDialogPanel m_loginDialog;    

    /**
     * Creates a new pane with the login panel.
     */
    RNodeDialogPane() {
        super();
        m_loginDialog = new RLoginDialogPanel();
    }
    
    /**
     * Adds the default login panel to the dialog. Useful for derived dialogs.
     */
    protected void addLoginTab() {
        super.addTab("Login", m_loginDialog);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_loginDialog.loadSettingsFrom(settings, specs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_loginDialog.saveSettingsTo(settings);
    }
    
}
