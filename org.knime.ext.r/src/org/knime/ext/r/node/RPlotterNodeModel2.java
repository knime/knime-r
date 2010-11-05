/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.local.RViewScriptingConstants;
import org.knime.ext.r.node.local.RViewsPngDialogPanel;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * This is the implementation of the R view plotting.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeModel2 extends RRemoteNodeModel {

    private Image m_resultImage;
    private File m_imageFile;

    /** PNG image output spec. */
    private static final ImagePortObjectSpec OUT_SPEC =
        new ImagePortObjectSpec(PNGImageContent.TYPE);

    private static final String FILE_NAME = "Rplot";

    // our LOGGER instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RPlotterNodeModel2.class);

    private SettingsModelIntegerBounded m_heightModel =
        RViewsPngDialogPanel.createHeightModel();

    private SettingsModelIntegerBounded m_widthModel =
        RViewsPngDialogPanel.createWidthModel();

    private SettingsModelIntegerBounded m_pointSizeModel =
        RViewsPngDialogPanel.createPointSizeModel();

    private SettingsModelString m_bgModel =
        RViewsPngDialogPanel.createBgModel();

    private String[] m_viewCmds =
            RViewScriptingConstants.getDefaultExpressionCommands();

    /**
     * Creates a new plotter with one data input.
     */
    protected RPlotterNodeModel2() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[] {ImagePortObject.TYPE});
        m_resultImage = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        RConnection c = getRconnection();
        // create unique png file name
        String fileName = FILE_NAME + "_" + System.identityHashCode(inData[0])
            + ".png";
        LOGGER.info("The image name: " + fileName);
        String pngCommand = "png(\"" + fileName + "\", width="
            + m_widthModel.getIntValue() + ", height="
            + m_heightModel.getIntValue() + ", pointsize="
            + m_pointSizeModel.getIntValue() + ", bg=\""
            + m_bgModel.getStringValue() + "\")";
        c.eval("try(" + pngCommand + ")");

        // send data to R server
        RConnectionRemote.sendData(c, (BufferedDataTable) inData[0], exec);

        // execute view command on server
        LOGGER.debug(Arrays.toString(m_viewCmds));
        exec.setMessage("Executing view R commands...");
        String[] parsedExp = parseExpression(m_viewCmds);
        for (String e : parsedExp) {
            LOGGER.debug("voidEval: try(" + e + ")");
            c.voidEval("try(" + e + ")");
        }
        c.voidEval("dev.off()");

        PNGImageContent content;
        try {
            // read png back from server
            RFileInputStream ris = c.openFile(fileName);
            m_imageFile = File.createTempFile(FILE_NAME, ".png");
            FileOutputStream out = new FileOutputStream(m_imageFile);
            FileUtil.copy(ris, out);
            FileInputStream in = new FileInputStream(m_imageFile);
            content = new PNGImageContent(in);
            in.close();
            m_resultImage = content.getImage();
        } finally {
            try {
                c.removeFile(fileName);
            } catch (RserveException e) {
                // ignore: file may not exist or is not removable
            }
            c.close();
        }
        // nothing, has no out-port
        return new PortObject[] {new ImagePortObject(content, OUT_SPEC)};
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new PortObjectSpec[] {OUT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_heightModel.saveSettingsTo(settings);
        m_widthModel.saveSettingsTo(settings);
        m_pointSizeModel.saveSettingsTo(settings);
        m_bgModel.saveSettingsTo(settings);

        RDialogPanel.setExpressionsTo(settings, m_viewCmds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_heightModel.loadSettingsFrom(settings);
        m_widthModel.loadSettingsFrom(settings);
        m_pointSizeModel.loadSettingsFrom(settings);
        m_bgModel.loadSettingsFrom(settings);

        m_viewCmds = RDialogPanel.getExpressionsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);

        String[] viewCmd = RDialogPanel.getExpressionsFrom(settings);

        // if command not valid throw exception
        if (viewCmd == null || viewCmd.length == 0) {
            throw new InvalidSettingsException("R View command is empty!");
        }

        m_heightModel.validateSettings(settings);
        m_widthModel.validateSettings(settings);
        m_pointSizeModel.validateSettings(settings);
        m_bgModel.validateSettings(settings);
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
        InputStream is = new FileInputStream(m_imageFile);
        m_resultImage = new PNGImageContent(is).getImage();
        is.close();
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
