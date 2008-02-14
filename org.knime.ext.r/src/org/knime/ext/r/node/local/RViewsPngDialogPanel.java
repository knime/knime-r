/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   27.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RViewsPngDialogPanel extends JLabel {
    
    /** Default tab title for this panel. */
    public static final String TAB_PNG_TITLE = "PNG Settings";
    
    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance 
     * containing the height of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createHeightModel() {
        return new SettingsModelIntegerBounded("R-png-height", 640, 
                50, Integer.MAX_VALUE);
    }
    
    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance 
     * containing the width of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createWidthModel() {
        return new SettingsModelIntegerBounded("R-png-width", 640, 
                50, Integer.MAX_VALUE);
    }

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance 
     * containing the point size of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createPointSizeModel() {
        return new SettingsModelIntegerBounded("R-png-pointSize", 12, 1, 
                Integer.MAX_VALUE);
    }
    
    /**
     * @return a <code>SettingsModelString</code> instance 
     * containing the background color of the png image to create by R.
     */
    public static final SettingsModelString createBgModel() {
        return new SettingsModelString("R-bg-col", "#FFFFFF");
    }    
    
    
    private DialogComponentNumberEdit m_heightComp;
    private SettingsModelIntegerBounded m_heightModel;
    
    private DialogComponentNumberEdit m_widthComp;
    private SettingsModelIntegerBounded m_widthModel;
    
    private DialogComponentNumber m_pointSizeComp;
    private SettingsModelIntegerBounded m_pointSizeModel;
    
    private DialogComponentString m_bgComp;
    private SettingsModelString m_bgModel;
    
    
    /**
     * Creates new instance of <code>RViewsPngDialogPanel</code> which provides
     * default dialog components to specify png options used by R to create
     * the png image.
     */
    public RViewsPngDialogPanel() {
        super();
        
        // Image size
        JPanel size = new JPanel();
        size.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Image size"));
        
        m_heightModel = createHeightModel();
        m_heightComp = new DialogComponentNumberEdit(m_heightModel, 
                "Image height");
        
        m_widthModel = createWidthModel();
        m_widthComp = new DialogComponentNumberEdit(m_widthModel, 
                "Image width");
        
        size.setLayout(new BorderLayout());
        size.add(m_heightComp.getComponentPanel(), BorderLayout.NORTH);
        size.add(m_widthComp.getComponentPanel(), BorderLayout.CENTER);
        
        // Point size
        m_pointSizeModel = createPointSizeModel();
        m_pointSizeComp = new DialogComponentNumber(m_pointSizeModel, 
                "Point size", 1);
        
        // Bg col
        m_bgModel = createBgModel();
        m_bgComp = new DialogComponentString(m_bgModel , "Background color");
        
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new BorderLayout());
        upperPanel.add(m_pointSizeComp.getComponentPanel(), BorderLayout.NORTH);
        upperPanel.add(m_bgComp.getComponentPanel(), BorderLayout.CENTER);
        
        // add all components
        setLayout(new BorderLayout());
        add(size, BorderLayout.NORTH);
        add(upperPanel, BorderLayout.CENTER);
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
        m_heightComp.loadSettingsFrom(settings, specs);
        m_widthComp.loadSettingsFrom(settings, specs);
        m_pointSizeComp.loadSettingsFrom(settings, specs);
        m_bgComp.loadSettingsFrom(settings, specs);
    }
    
    /**
     * Saves settings set in the dialog components into the settings instance.
     * @param settings The settings instance ot save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        m_heightComp.saveSettingsTo(settings);
        m_widthComp.saveSettingsTo(settings);
        m_pointSizeComp.saveSettingsTo(settings);
        m_bgComp.saveSettingsTo(settings);
    }
}
