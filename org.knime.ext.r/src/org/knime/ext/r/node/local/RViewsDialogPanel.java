/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   24.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
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
    static final SettingsModelString createViewSettingsModel() {
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
                .createEtchedBorder(), "R command"));
        this.add(m_viewSelectionComponent.getComponentPanel(), 
                BorderLayout.NORTH);        
        this.add(m_commandPanel, BorderLayout.CENTER);
    }
    
    /**
     * Loads settings into dialog components.
     * @param settings The settings to load.
     * @param specs The specs of the input data table.
     * @throws NotConfigurableException If components could not be configured
     * and settings not be set. 
     */
    public void loadSettings(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_viewSelectionComponent.loadSettingsFrom(settings, specs);
        m_commandPanel.loadSettingsFrom(settings, specs);
    }
    
    /**
     * Saves settings set in the dialog components into the settings instance.
     * @param settings The settings instance ot save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        m_viewSelectionComponent.saveSettingsTo(settings);
        m_commandPanel.saveSettingsTo(settings);
    }
    
    
    /**
     * Listener to react on selection changes made in the drop down menu.
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class ViewChangeListener implements ChangeListener {

        /**
         * Shows up the related dummy code of the chosen R view 
         * in the multi line text field when the selection of drop down menu 
         * changes. 
         * 
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            m_commandPanel.setText(
                    RViewScriptingConstants.LABEL2COMMAND.get(
                            m_viewSettingsModel.getStringValue()));
        }
    }   
}
