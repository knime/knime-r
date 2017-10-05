/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.local.RViewScriptingConstants;
import org.knime.ext.r.node.local.RViewsDialogPanel;
import org.knime.ext.r.node.local.RViewsPngDialogPanel;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * This is the implementation of the R view plotting.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@Deprecated
public class RPlotterNodeModel extends RRemoteNodeModel {

    private Image m_resultImage;

    private File m_imageFile;

    private static final String FILE_NAME = "Rplot";

    // our LOGGER instance
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RPlotterNodeModel.class);

    private final SettingsModelIntegerBounded m_heightModel = RViewsPngDialogPanel.createHeightModel();

    private final SettingsModelIntegerBounded m_widthModel = RViewsPngDialogPanel.createWidthModel();

    private final SettingsModelString m_resolutionModel = RViewsPngDialogPanel.createResolutionModel();

    private final SettingsModelIntegerBounded m_pointSizeModel = RViewsPngDialogPanel.createPointSizeModel();

    private final SettingsModelString m_bgModel = RViewsPngDialogPanel.createBgModel();

    private final SettingsModelString m_viewType = RViewsDialogPanel.createViewSettingsModel();

    private String[] m_viewCmds = RViewScriptingConstants.getDefaultExpressionCommands();

    /**
     * Creates a new plotter with one data input.
     */
    protected RPlotterNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[0]);
        m_resultImage = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final RConnection c = getRconnection();
        // create unique png file name
        final String fileName = FILE_NAME + "_" + System.identityHashCode(inData[0]) + ".png";
        LOGGER.info("The image name: " + fileName);
        final String pngCommand = "png(\"" + fileName + "\"" + ", width=" + m_widthModel.getIntValue() + ", height="
            + m_heightModel.getIntValue() + ", pointsize=" + m_pointSizeModel.getIntValue() + ", bg=\""
            + m_bgModel.getStringValue() + "\"" + ", res=" + m_resolutionModel.getStringValue() + ")";
        c.eval("try(" + pngCommand + ")");

        // send data to R server
        RConnectionRemote.sendData(c, (BufferedDataTable)inData[0], exec);

        // execute view command on server
        LOGGER.debug(Arrays.toString(m_viewCmds));
        exec.setMessage("Executing view R commands...");
        final String[] parsedExp = parseExpression(m_viewCmds);
        for (final String e : parsedExp) {
            LOGGER.debug("voidEval: try(" + e + ")");
            c.voidEval("try(" + e + ")");
        }
        c.voidEval("dev.off()");

        try {
            // read png back from server
            final RFileInputStream ris = c.openFile(fileName);
            m_imageFile = File.createTempFile(FILE_NAME, ".png", new File(KNIMEConstants.getKNIMETempDir()));
            final FileOutputStream out = new FileOutputStream(m_imageFile);
            FileUtil.copy(ris, out);
            final FileInputStream in = new FileInputStream(m_imageFile);
            m_resultImage = createImage(in);
            in.close();
        } finally {
            try {
                c.removeFile(fileName);
            } catch (final RserveException e) {
                // ignore: file may not exist or is not removable
            }
            c.close();
        }
        // nothing, has no out-port
        return new BufferedDataTable[0];
    }

    /**
     * Creates an image instance out of the given <code>InputStream</code>. This stream can for instance a
     * <code>FileInputStream</code> holding a image file, such as a png and so on.
     *
     * @param is The stream reading the image file.
     * @return The image instance.
     * @throws IOException If image file can no be red.
     */
    public static Image createImage(final InputStream is) throws IOException {
        final Vector<byte[]> buffers = new Vector<byte[]>();
        final int bufSize = 65536;
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
        final byte[] imgCode = new byte[imgLength];
        int imgPos = 0;
        for (final Enumeration<byte[]> e = buffers.elements(); e.hasMoreElements();) {
            final byte[] b = e.nextElement();
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_heightModel.saveSettingsTo(settings);
        m_widthModel.saveSettingsTo(settings);
        m_resolutionModel.saveSettingsTo(settings);
        m_pointSizeModel.saveSettingsTo(settings);
        m_bgModel.saveSettingsTo(settings);
        RDialogPanel.setExpressionsTo(settings, m_viewCmds);
        m_viewType.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_heightModel.loadSettingsFrom(settings);
        m_widthModel.loadSettingsFrom(settings);
        try {
            m_resolutionModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ise) {
            // ignore backward comp. < v2.3.1
        }
        m_pointSizeModel.loadSettingsFrom(settings);
        m_bgModel.loadSettingsFrom(settings);
        m_viewCmds = RDialogPanel.getExpressionsFrom(settings);
        try {
            m_viewType.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ise) {
            // ignore backward comp. < v2.3
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);

        final String[] viewCmd = RDialogPanel.getExpressionsFrom(settings);

        // if command not valid throw exception
        if ((viewCmd == null) || (viewCmd.length == 0)) {
            throw new InvalidSettingsException("R View command is empty!");
        }

        m_heightModel.validateSettings(settings);
        m_widthModel.validateSettings(settings);
        // new with 2.3.1: no validation possible
        // m_resolutionModel.validateSettings(settings);
        m_pointSizeModel.validateSettings(settings);
        m_bgModel.validateSettings(settings);
    }

    /**
     * @return result image for the view, only available after successful evaluation
     */
    Image getResultImage() {
        return m_resultImage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final File file = new File(nodeInternDir, FILE_NAME + ".png");
        m_imageFile = File.createTempFile(FILE_NAME, ".png", new File(KNIMEConstants.getKNIMETempDir()));
        FileUtil.copy(file, m_imageFile);
        m_resultImage = createImage(new FileInputStream(m_imageFile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final File file = new File(nodeInternDir, FILE_NAME + ".png");
        FileUtil.copy(m_imageFile, file);
    }

}
