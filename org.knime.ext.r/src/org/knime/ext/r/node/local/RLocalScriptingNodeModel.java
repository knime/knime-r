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
 *   17.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalScriptingNodeModel extends RLocalNodeModel {

    private SettingsModelString m_commandModel = 
        RLocalScriptingNodeDialogPane.createCommandSettingsModel(); 
    
    /**
     * Creates new instance of <code>RLocalScriptingNodeModel</code>. 
     */
    public RLocalScriptingNodeModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCommand() {
        return m_commandModel.getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // Check something ... ???
        
        return new DataTableSpec[1];
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        readSettings(settings, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_commandModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        readSettings(settings, true);
    }

    private void readSettings(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {
        SettingsModelString tempCommand = 
            m_commandModel.createCloneWithValidatedValue(settings);
        String tempCommandString = tempCommand.getStringValue();
        
        // if command not valid throw exception
        if (tempCommandString.length() < 1) {
            throw new InvalidSettingsException("R command is not valid !");
        }
        
        if (!validateOnly) {
            m_commandModel.loadSettingsFrom(settings);
        }
    }
}
