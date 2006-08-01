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

import java.awt.GridLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodedialog.DialogComponentInteger;
import org.knime.core.node.defaultnodedialog.DialogComponentPasswordField;
import org.knime.core.node.defaultnodedialog.DialogComponentTextField;


/**
 * Panel used to login to a R server providing user, password, host, and port.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class RLoginDialogPanel extends JPanel {

    private final DialogComponentTextField m_host;

    private final DialogComponentInteger m_port;

    private final DialogComponentTextField m_user;

    private final DialogComponentPasswordField m_pass;

    public RLoginDialogPanel() {
        super(new GridLayout(4, 1));
        m_host = new DialogComponentTextField(RConstants.KEY_HOST, "Host: ",
                RConstants.DEFAULT_HOST);
        m_host.setSizeComponents(200, 25);
        m_port = new DialogComponentInteger(RConstants.KEY_PORT, "Port: ", RConstants.DEFAULT_PORT);
        m_port.setSizeComponents(200, 25);
        m_user = new DialogComponentTextField(RConstants.KEY_USER, "User: ", RConstants.DEFAULT_USER);
        m_user.setSizeComponents(200, 25);
        m_pass = new DialogComponentPasswordField(RConstants.KEY_PASSWORD,
                "Password: ");
        m_pass.setSizeComponents(200, 25);
        super.add(m_host);
        super.add(m_port);
        super.add(m_user);
        super.add(m_pass);
    }

    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        m_host.loadSettingsFrom(settings, specs);
        m_port.loadSettingsFrom(settings, specs);
        m_user.loadSettingsFrom(settings, specs);
        // password is not loaded since it is not saved by the model
    }

    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_host.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_user.saveSettingsTo(settings);
        m_pass.saveSettingsTo(settings);
    }

}
