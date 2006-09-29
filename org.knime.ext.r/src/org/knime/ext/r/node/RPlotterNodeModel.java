/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
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
import java.io.IOException;
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
import org.rosuda.JRclient.REXP;
import org.rosuda.JRclient.RFileInputStream;
import org.rosuda.JRclient.RSrvException;
import org.rosuda.JRclient.Rconnection;

/**
 * This is the implementation of the R 2D view ploting two columns in a
 * Scatterplot.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeModel extends RNodeModel {

    private String[] m_cols = new String[0];

    private Image m_resultImage;
    
    private static final String FILE_NAME = "Rplot";

    // our LOGGER instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RPlotterNodeModel.class);

    /**
     * 
     * 
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
        LOGGER.debug("File: " + fileName);
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
        
//        // ok, so the device should be fine - let's plot
//        String[] lines = m_expression.split("\n");
//        for (int i = 0; i < lines.length; i++) {
//            LOGGER.debug("eval: " + lines[i]);
//            xp = c.eval(lines[i]);
//        }
        
        // the file should be ready now, so let's read (ok this isn't pretty,
        // but hey, this ain't no beauty contest *grin* =)
        // we read in chunks of bufSize (64k by default) and store the resulting
        // byte arrays in a vector
        // ... just in case the file gets really big ...
        // we don't know the size in advance, because it's just a stream.
        // also we can't rewind it, so we have to store it piece-by-piece
        RFileInputStream is = c.openFile(fileName);
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
        LOGGER.info("The image " + fileName + " has " + imgLength + " bytes.");

        // now let's join all the chunks into one, big array ...
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

        // close the file and remove it
        is.close();
        try {
            c.removeFile(fileName);
        } catch (RSrvException e) {
            // ignore
        } finally {
            c.close();
        }

        // now this is pretty boring AWT stuff, nothing to do with R ...
        m_resultImage = Toolkit.getDefaultToolkit().createImage(imgCode);

        // nothing
        return new BufferedDataTable[0];
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_resultImage = null;
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkRconnection();
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
        
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {

    }
}
