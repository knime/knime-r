/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2010
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
 */
package org.knime.ext.r.node.local;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.r.node.RDialogPanel;

/**
 * <code>RViewsDialogPanel</code> is a <code>JPanel</code> which provides
 * a combo box containing names of available R plots and an editor pane to
 * insert R code.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RViewsDialogPanel extends JPanel {

    /**
     * @return a <code>SettingsModelString</code> instance containing
     * a set of names of R views.
     */
    public static final SettingsModelString createViewSettingsModel() {
        return new SettingsModelString("R_View",
                RViewScriptingConstants.DFT_EXPRESSION_KEY);
    }

    private final SettingsModelString m_viewSettingsModel;
    private final DialogComponentStringSelection m_viewSelectionComponent;

    private final RDialogPanel m_commandPanel;


    /**
     * Creates new instance of <code>RViewsDialogPanel</code>.
     */
    public RViewsDialogPanel() {
        super(new BorderLayout());

        Set<String> keys = RViewScriptingConstants.LABEL2COMMAND.keySet();
        List<String> list = new ArrayList<String>(keys);

        m_commandPanel = new RDialogPanel();

        m_viewSettingsModel = createViewSettingsModel();
        m_viewSettingsModel.addChangeListener(new ViewChangeListener());
        m_viewSelectionComponent = new DialogComponentStringSelection(
                m_viewSettingsModel, "View type", list);

        this.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "R Command"));
        this.add(m_viewSelectionComponent.getComponentPanel(),
                BorderLayout.NORTH);
        this.add(m_commandPanel, BorderLayout.CENTER);
    }

    /**
     * Loads settings into dialog components.
     * @param settings The settings to load.
     * @param specs The specs of the input data table.
     * @param map of flow variables together with its identifiers
     * @throws NotConfigurableException If components could not be configured
     * and settings not be set.
     */
    public void loadSettings(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final Map<String, FlowVariable> map)
            throws NotConfigurableException {
        m_viewSelectionComponent.loadSettingsFrom(settings, specs);
        m_commandPanel.loadSettingsFrom(settings, specs, map);
    }

    /**
     * Saves settings set in the dialog components into the settings instance.
     * @param settings The settings instance to save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        m_viewSelectionComponent.saveSettingsTo(settings);
        m_commandPanel.saveSettingsTo(settings);
    }


    /**
     * Listener to react on selection changes made in the drop down menu.
     */
    private class ViewChangeListener implements ChangeListener {

        /**
         * Shows up the related dummy code of the chosen R view
         * in the multi line text field when the selection of drop down menu
         * changes.
         *
         * {@inheritDoc}
         */
        @Override
        public void stateChanged(final ChangeEvent e) {
            m_commandPanel.setText(
                    RViewScriptingConstants.LABEL2COMMAND.get(
                            m_viewSettingsModel.getStringValue()));
        }
    }
}
