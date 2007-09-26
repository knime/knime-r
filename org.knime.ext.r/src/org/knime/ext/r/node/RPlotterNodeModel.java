/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *   30.09.2005 (Florian Georg): created
 */
package org.knime.ext.r.node;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.local.RLocalViewsNodeDialog;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * This is the implementation of the R view plotting.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeModel extends RNodeModel {

    private Image m_resultImage;
    private File m_imageFile;
    
    private static final String FILE_NAME = "Rplot";

    // our LOGGER instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RPlotterNodeModel.class);

    
    private SettingsModelString m_viewModel = 
        RLocalViewsNodeDialog.createViewSettingsModel(); 
    
    private SettingsModelFilterString m_colFilterModel = 
        RLocalViewsNodeDialog.createColFilterSettingsModel();
    
    private SettingsModelString m_viewCmdModel = 
        RLocalViewsNodeDialog.createRViewCmdSettingsModel();    
    
    
    /**
     * Creates a new plotter with one data input. 
     */
    protected RPlotterNodeModel() {
        super(1, 0);
        m_resultImage = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        List<String> includeList = m_colFilterModel.getIncludeList();
        // Filter columns before processing
        ColumnRearranger cr = new ColumnRearranger(
                inData[0].getDataTableSpec());
        cr.keepOnly(includeList.toArray(new String[includeList.size()]));
        BufferedDataTable dataTableToUse = exec.createColumnRearrangeTable(
                inData[0], cr, exec);
        // create unique png file
        RConnection c = getRconnection();
        String fileName = FILE_NAME + "_" + System.identityHashCode(cr) 
            + ".png";
        LOGGER.info("The image name: " + fileName);
        c.eval("try(png(\"" + fileName + "\"))");
        
        // send data to R server
        RConnectionRemote.sendData(c, dataTableToUse, exec);

        // execute view command on server
        LOGGER.info(m_viewCmdModel.getStringValue());
        c.eval("try(" + m_viewCmdModel.getStringValue() + ")");
        c.voidEval("dev.off()");
        
        // read png back from server
        InputStream ris = c.openFile(fileName);
        m_imageFile = File.createTempFile(FILE_NAME, ".png");
        FileOutputStream copy = new FileOutputStream(m_imageFile);
        FileUtil.copy(ris, copy);
        FileInputStream in = new FileInputStream(m_imageFile);
        try {
            m_resultImage = createImage(in);
            // close stream and remove it at the server
            in.close();
            c.removeFile(fileName);
        } catch (RserveException e) {
            // ignore
        } finally {
            c.close();
        }

        // nothing
        return new BufferedDataTable[0];
    }
    
    /**
     * Creates an image instance out of the given <code>InputStream</code>.
     * This stream can for instance a <code>FileInputStream</code> holding
     * a image file, such as a .png and so on. 
     * 
     * @param is The stream reading the image file.
     * @return The image instance.
     * @throws IOException If image file can no be red.
     */
    public static Image createImage(final InputStream is) throws IOException {
        Vector<byte[]> buffers = new Vector<byte[]>();
        int bufSize = 65536;
        byte[] buf = new byte[bufSize];
        int imgLength = 0;
        int n = 0;
        while (true) {
            n = is.read(buf);
            if (n == bufSize) {
                buffers.addElement(buf);
                buf = new byte[bufSize];
            }
            if (n > 0) {
                imgLength += n;
            }
            if (n < bufSize) {
                break;
            }
        }
        LOGGER.info("The image has " + imgLength + " bytes.");  
        byte[] imgCode = new byte[imgLength];
        int imgPos = 0;
        for (Enumeration<byte[]> e = buffers.elements(); e.hasMoreElements();) {
            byte[] b = (byte[]) e.nextElement();
            System.arraycopy(b, 0, imgCode, imgPos, bufSize);
            imgPos += bufSize;
        }
        if (n > 0) {
            System.arraycopy(buf, 0, imgCode, imgPos, n);
        }

        // create image based on image code
        return Toolkit.getDefaultToolkit().createImage(imgCode);
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        if (m_resultImage != null) {
            m_resultImage.flush();
            m_resultImage = null;
        } 
        if (m_imageFile != null) {
            m_imageFile.deleteOnExit();
            m_imageFile.delete();
            m_imageFile = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        //getRconnection();
        return new DataTableSpec[0];
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_viewModel.loadSettingsFrom(settings);
        m_colFilterModel.loadSettingsFrom(settings);
        m_viewCmdModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_viewModel.validateSettings(settings);
        m_colFilterModel.validateSettings(settings);
        m_viewCmdModel.validateSettings(settings);
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
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        File file = new File(nodeInternDir, FILE_NAME + ".png");
        m_imageFile = File.createTempFile(FILE_NAME, ".png");
        FileUtil.copy(file, m_imageFile);
        m_resultImage = createImage(new FileInputStream(m_imageFile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        File file = new File(nodeInternDir, FILE_NAME + ".png");
        FileUtil.copy(m_imageFile, file);
    }
    
}
