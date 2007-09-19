/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   17.09.2007 (gabriel): created
 */
package org.knime.ext.r.node.local;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @author Kilian Thiel, University of Konstanz
 */
public abstract class RLocalNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * @return Returns a <code>SettingsModelString</code> instance containing
     * the path the R executable file.
     */
    static final SettingsModelString createRBinaryFile() {
        return new SettingsModelString("R_binary_file", 
                RPreferenceInitializer.getRPath()); 
    }
    
    /**
     * @return Returns a <code>SettingsModelBoolean</code> instance specifying
     * if the determined R executable file is used.
     */
    static final SettingsModelBoolean createUseSpecifiedFileModel() {
        return new SettingsModelBoolean("R_use_specified_file", false);
    }
    
    private SettingsModelBoolean m_smb; 
    
    private SettingsModelString m_fileModel;
    
    /**
     * Constructor of <code>RLocalNodeDialogPane</code> which provides a
     * default dialog component to specify the R executable file.  
     */
    public RLocalNodeDialogPane() {
        super();
        
        m_fileModel = createRBinaryFile();
        m_smb = createUseSpecifiedFileModel();
        m_smb.addChangeListener(new CheckBoxChangeListener());
        
        DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
                m_fileModel, "R_binarys", JFileChooser.OPEN_DIALOG, 
                false, new String[]{"", "exe"});

        setHorizontalPlacement(true);
        createNewGroup("R binary");
        addDialogComponent(new DialogComponentBoolean(m_smb, 
                "R path"));
        addDialogComponent(fileChooser);
        closeCurrentGroup();
        setHorizontalPlacement(false);
        
        enableFileChooser();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        enableFileChooser();
    }
    
    
    /**
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class CheckBoxChangeListener implements ChangeListener {

        /**
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            enableFileChooser();
        }
    }
    
    private void enableFileChooser() {
        if (!m_smb.getBooleanValue()) {
            m_fileModel.setEnabled(false);
        } else {
            m_fileModel.setEnabled(true);
        }
    }
}
