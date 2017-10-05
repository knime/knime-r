/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
package org.knime.ext.r.node;

import java.awt.GridLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * Panel used to login to a R server providing user, password, host, and port.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("serial")
final class RLoginDialogPanel extends JPanel {

    private final DialogComponentString m_host = new DialogComponentString(RLoginSettings.createHostModel(), "Host: ");

    private final DialogComponentNumber m_port =
        new DialogComponentNumber(RLoginSettings.createPortModel(), "Port: ", 1);

    private final DialogComponentString m_user = new DialogComponentString(RLoginSettings.createUserModel(), "User: ");

    private final DialogComponentPasswordField m_pass =
        new DialogComponentPasswordField(RLoginSettings.createPasswordModel(), "Password: ");

    /**
     * Default constructor.
     */
    public RLoginDialogPanel() {
        super(new GridLayout(4, 1));
        super.add(m_host.getComponentPanel());
        super.add(m_port.getComponentPanel());
        super.add(m_user.getComponentPanel());
        super.add(m_pass.getComponentPanel());
    }

    /**
     * Transfers the values from the specified settings object into the dialog components.
     *
     * @param settings the new settings to display in the components.
     * @param specs the table specs from the input ports.
     * @throws NotConfigurableException if settings can't be loaded
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_host.loadSettingsFrom(settings, specs);
        m_port.loadSettingsFrom(settings, specs);
        m_user.loadSettingsFrom(settings, specs);
        m_pass.loadSettingsFrom(settings, specs);
    }

    /**
     * Saves the current values in the dialog components into the specified settings object.
     *
     * @param settings the object to write the current settings into.
     * @throws InvalidSettingsException if the current values are invalid.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_host.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_user.saveSettingsTo(settings);
        m_pass.saveSettingsTo(settings);
    }

}
