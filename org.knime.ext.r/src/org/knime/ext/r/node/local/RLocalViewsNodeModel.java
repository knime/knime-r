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
 *   18.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.RPlotterNodeModel;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeModel extends RLocalNodeModel {
    
    private static final String INTERNAL_FILE_NAME = "Rplot";
    
    
    private SettingsModelString m_viewModel = 
        RLocalViewsNodeDialog.createViewSettingsModel(); 
    
    private SettingsModelFilterString m_colFilterModel = 
        RLocalViewsNodeDialog.createColFilterSettingsModel();
    
    private SettingsModelString m_viewCmdModel = 
        RLocalViewsNodeDialog.createRViewCmdSettingsModel();
    
    private Image m_resultImage;
    
    private String m_filename;
    
    
    /**
     * Creates new instance of <code>RLocalViewsNodeModel</code>. 
     */
    public RLocalViewsNodeModel() {
        super(1, 0);
     
        m_resultImage = null;
    }
   
    
    /**
     * @return result image for the view, only available after successful
     *         evaluation
     */
    Image getResultImage() {
        return m_resultImage;
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCommand() {
        return "png(\"" + m_filename + "\");\n" 
              + m_viewCmdModel.getStringValue() 
              + "\ndev.off();";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final BufferedDataTable[] postprocessDataTable(
            final BufferedDataTable[] outData, final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        
        // create image after execution.
        m_resultImage = RPlotterNodeModel.createImage(
                new FileInputStream(new File(m_filename)));
        
        return new BufferedDataTable[]{};
    } 
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final BufferedDataTable[] preprocessDataTable(
            final BufferedDataTable[] inData, final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        
        m_filename = TEMP_PATH + "/" + "R-View-" 
        + System.identityHashCode(inData) + ".png";
               
        List<String> includeList = m_colFilterModel.getIncludeList();
        
        ColumnRearranger cr = new ColumnRearranger(
                inData[0].getDataTableSpec());
        cr.keepOnly(includeList.toArray(new String[includeList.size()]));
        BufferedDataTable dataTableToUse = exec.createColumnRearrangeTable(
                inData[0], cr, exec);
        
        return new BufferedDataTable[]{dataTableToUse};
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
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
        m_viewModel.saveSettingsTo(settings);
        m_colFilterModel.saveSettingsTo(settings);
        m_viewCmdModel.saveSettingsTo(settings);
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
        SettingsModelString tempView = 
            m_viewModel.createCloneWithValidatedValue(settings);
        String tempViewStr = tempView.getStringValue();
        
        // if command not valid throw exception
        if (tempViewStr.length() < 1) {
            throw new InvalidSettingsException("R View is not valid !");
        }
        
        if (!validateOnly) {
            m_viewModel.loadSettingsFrom(settings);
            m_colFilterModel.loadSettingsFrom(settings);
            m_viewCmdModel.loadSettingsFrom(settings);
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);
        
        File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
        File pngFile = File.createTempFile(INTERNAL_FILE_NAME, ".png");
        FileUtil.copy(file, pngFile);
        m_resultImage = RPlotterNodeModel.createImage(
                new FileInputStream(pngFile));
        m_filename = file.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);
        
        File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
        FileUtil.copy(new File(m_filename), file);
    }   
}
