/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * <code>RViewsDialogPanel</code> is a <code>JPanel</code> which provides a combo box containing names of available R
 * plots and an editor pane to insert R code.
 *
 * @author Kilian Thiel, University of Konstanz
 */
@SuppressWarnings("serial")
public class RViewsDialogPanel extends JPanel {

    /**
     * @return a <code>SettingsModelString</code> instance containing a set of names of R views.
     */
    public static final SettingsModelString createViewSettingsModel() {
        return new SettingsModelString("R_View", RViewScriptingConstants.DFT_EXPRESSION_KEY);
    }

    private final SettingsModelString m_viewSettingsModel;

    private final DialogComponentStringSelection m_viewSelectionComponent;

    private final RDialogPanel m_commandPanel;

    /**
     * Creates new instance of <code>RViewsDialogPanel</code>.
     */
    public RViewsDialogPanel() {
        super(new BorderLayout());

        final Set<String> keys = RViewScriptingConstants.LABEL2COMMAND.keySet();
        final List<String> list = new ArrayList<String>(keys);

        m_commandPanel = new RDialogPanel();

        m_viewSettingsModel = createViewSettingsModel();
        m_viewSettingsModel.addChangeListener(new ViewChangeListener());
        m_viewSelectionComponent = new DialogComponentStringSelection(m_viewSettingsModel, "View type", list);

        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "R Command"));
        this.add(m_viewSelectionComponent.getComponentPanel(), BorderLayout.NORTH);
        this.add(m_commandPanel, BorderLayout.CENTER);
    }

    /**
     * Loads settings into dialog components.
     * 
     * @param settings The settings to load.
     * @param specs The specs of the input data table.
     * @param map of flow variables together with its identifiers
     * @throws NotConfigurableException If components could not be configured and settings not be set.
     */
    public void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final Map<String, FlowVariable> map) throws NotConfigurableException {
        m_viewSelectionComponent.loadSettingsFrom(settings, specs);
        m_commandPanel.loadSettingsFrom(settings, specs, map);
    }

    /**
     * Saves settings set in the dialog components into the settings instance.
     * 
     * @param settings The settings instance to save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_viewSelectionComponent.saveSettingsTo(settings);
        m_commandPanel.saveSettingsTo(settings);
    }

    /**
     * Listener to react on selection changes made in the drop down menu.
     */
    private class ViewChangeListener implements ChangeListener {

        /**
         * Shows up the related dummy code of the chosen R view in the multi line text field when the selection of drop
         * down menu changes.
         *
         * {@inheritDoc}
         */
        @Override
        public void stateChanged(final ChangeEvent e) {
            m_commandPanel.setText(RViewScriptingConstants.LABEL2COMMAND.get(m_viewSettingsModel.getStringValue()));
        }
    }
}
