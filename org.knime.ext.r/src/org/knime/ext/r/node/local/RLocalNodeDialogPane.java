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
 * The <code>RLocalNodeDialogPane</code> is a
 * dialog pane providing a file chooser to select the R executable, as well
 * as a checkbox to specify which R executable will be used to execute the
 * R script. If the checkbox is <u>not</u> checked, the R executable file
 * specified in the KNIME-R preferences is used, if the checkbox <u>is</u>
 * checked the specified file of the file chooser dialog is used. 
 * This dialog can be extended to take use of this functionality but be aware
 * to call the super constructor when extending 
 * <code>RLocalNodeDialogPane</code>.
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
    
    private final SettingsModelBoolean m_smb; 
    
    private final SettingsModelString m_fileModel;
    
    /**
     * Constructor of <code>RLocalNodeDialogPane</code> which provides a
     * default dialog component to specify the R executable file and a checkbox
     * to specify which R executable is used.
     */
    public RLocalNodeDialogPane() {
        super();
        
        // create setting models and add listener to model of checkbox.
        m_fileModel = createRBinaryFile();
        m_smb = createUseSpecifiedFileModel();
        m_smb.addChangeListener(new CheckBoxChangeListener());
        
        // create file chooser component.
        DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
                m_fileModel, "R_binarys", JFileChooser.OPEN_DIALOG, 
                false, new String[]{"", "exe"});

        setHorizontalPlacement(true);
        createNewGroup("R binary path");
        
        // create check box component
        DialogComponentBoolean checkbox = new DialogComponentBoolean(
                m_smb, "Use selected");
        checkbox.setToolTipText("If checked, the specified file is used " 
                + "as R binary. If not checked the file specified in " 
                + "the KNIME's R preferences is used.");
        
        addDialogComponent(checkbox);
        addDialogComponent(fileChooser);
        
        closeCurrentGroup();
        setHorizontalPlacement(false);
        
        enableFileChooser();
    }
    
    /**
     * Enable or disable file chooser model.
     * 
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
    
    /**
     * Enables the file chooser model if checkbox is checked and disables it
     * when the checkbox is not checked.
     */
    private void enableFileChooser() {
        if (!m_smb.getBooleanValue()) {
            m_fileModel.setEnabled(false);
        } else {
            m_fileModel.setEnabled(true);
        }
    }
}
