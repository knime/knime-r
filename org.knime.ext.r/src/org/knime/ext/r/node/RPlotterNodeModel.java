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
import java.util.Vector;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.rosuda.JRclient.REXP;
import org.rosuda.JRclient.RSrvException;
import org.rosuda.JRclient.Rconnection;

/**
 * This is the implementation of the R 2D view plotting two columns in a
 * Scatterplot.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeModel extends RNodeModel {

    private String[] m_cols = new String[0];

    private Image m_resultImage;
    private File m_imageFile;
    
    private static final String FILE_NAME = "Rplot";

    // our LOGGER instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RPlotterNodeModel.class);

    /**
     * Creates a new plotter with one data input. 
     */
    protected RPlotterNodeModel() {
        super(1, 0);
        m_resultImage = null;
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // we are careful here - not all R binaries support png
        // so we rather capture any failures
        Rconnection c = getRconnection();
        String fileName = FILE_NAME + "_" + c.hashCode() + ".png";
        LOGGER.info("The image name: " + fileName);
        REXP xp = c.eval("try(png(\"" + fileName + "\"))");

        if (xp.asString() != null) { // if there's a string then we have a
            // problem, R sent an error
            LOGGER.warn("Can't open png graphics device:\n"
                    + xp.asString());
            // this is analogous to 'warnings', but for us it's sufficient to
            // get just the 1st warning
            REXP w = c.eval("if (exists(\"last.warning\") && "
                    + "length(last.warning)>0) names(last.warning)[1] "
                    + "else 0");
            if (w.asString() != null) {
                LOGGER.warn(w.asString());
            }
        }
        
        RConnection.sendData(c, inData[0], exec);
        String first = RConnection.formatColumn(m_cols[0]);
        String second = RConnection.formatColumn(m_cols[1]);
        c.eval("plot(" + first + "," + second + ")");
        c.voidEval("dev.off()");
        
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
        } catch (RSrvException e) {
            // ignore
        } finally {
            c.close();
        }

        // nothing
        return new BufferedDataTable[0];
    }
    
    private Image createImage(final InputStream is) throws IOException {
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
        for (Enumeration e = buffers.elements(); e.hasMoreElements();) {
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
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        //checkRconnection();
        if (m_cols == null) {
            throw new InvalidSettingsException("No columns selected.");
        }
        if (m_cols.length != 2) {
            throw new InvalidSettingsException("Two columns need to be "
                    + "selected: " + m_cols.length);
        }
        int first = inSpecs[0].findColumnIndex(m_cols[0]);
        int second = inSpecs[0].findColumnIndex(m_cols[1]);
        if (first > -1 && second > -1 && first != second) {
            if (0 <= first && first < inSpecs[0].getNumColumns()) {
                if (0 <= second && second < inSpecs[0].getNumColumns()) {
                    return new DataTableSpec[0];
                }
            }
        }
        throw new InvalidSettingsException("Columns " + m_cols[0] + " and " 
                + m_cols[1] + " not found.");
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addStringArray("PLOT", m_cols);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_cols = settings.getStringArray("PLOT");
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        String[] selCells = settings.getStringArray("PLOT");
        if (selCells == null) {
            throw new InvalidSettingsException("No columns selected.");
        }
        if (selCells.length != 2) {
            throw new InvalidSettingsException("Two columns need to be "
                    + "selected: " + selCells.length);
        }

    }

    /**
     * @return result image for the view, only available after successful
     *         evaluation
     */
    Image getResultImage() {
        return m_resultImage;
    }
    
    /**
     * @see org.knime.core.node.NodeModel#loadInternals(
     * File, ExecutionMonitor)
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
     * @see org.knime.core.node.NodeModel#saveInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        File file = new File(nodeInternDir, FILE_NAME + ".png");
        FileUtil.copy(m_imageFile, file);
    }
    
}
