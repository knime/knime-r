/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node.local;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
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
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.preferences.RPreferenceProvider;

/**
 * The <code>RLocalViewsNodeModel</code> provides functionality to create a R script with user defined R code calling R
 * plots, run it and display the generated plot in the nodes view.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeModel2 extends RLocalNodeModel {

    /**
     * Default image size.
     */
    public static final int IMG_DEF_SIZE = 640;

    /**
     * Minimum image width.
     */
    public static final int IMG_MIN_WIDTH = 50;

    /**
     * Maximum image width.
     */
    public static final int IMG_MAX_WIDTH = Integer.MAX_VALUE;

    /**
     * Minimum image height.
     */
    public static final int IMG_MIN_HEIGHT = 50;

    /**
     * Maximum image height.
     */
    public static final int IMG_MAX_HEIGHT = Integer.MAX_VALUE;

    private static final String INTERNAL_FILE_NAME = "Rplot";

    /** Output spec for a PNG image. */
    private static final ImagePortObjectSpec OUT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    private final SettingsModelIntegerBounded m_heightModel = RViewsPngDialogPanel.createHeightModel();

    private final SettingsModelIntegerBounded m_widthModel = RViewsPngDialogPanel.createWidthModel();

    private final SettingsModelString m_resolutionModel = RViewsPngDialogPanel.createResolutionModel();

    private final SettingsModelIntegerBounded m_pointSizeModel = RViewsPngDialogPanel.createPointSizeModel();

    private final SettingsModelString m_bgModel = RViewsPngDialogPanel.createBgModel();

    private final SettingsModelString m_viewType = RViewsDialogPanel.createViewSettingsModel();

    private Image m_resultImage;

    private String m_filename;

    private String m_viewCmd = RViewScriptingConstants.getDefaultExpressionCommand();

    /**
     * Creates new instance of <code>RLocalViewsNodeModel</code> with one data in port and no data out port.
     * 
     * @param pref provider for R executable
     */
    public RLocalViewsNodeModel2(final RPreferenceProvider pref) {
        super(new PortType[]{ImagePortObject.TYPE}, pref);
        m_resultImage = null;
    }

    /**
     * @return result image for the view, only available after successful execution of the node model.
     */
    Image getResultImage() {
        return m_resultImage;
    }

    /**
     * Provides the R code to run, consisting of the <code>png()</code> command to create a new png file, the plot
     * command specified by the user and the <code>dev.off()</code> command to shut down the standard graphic device.
     *
     * {@inheritDoc}
     */
    @Override
    protected String getCommand() {
        return "png(\"" + m_filename + "\"" + ", width=" + m_widthModel.getIntValue() + ", height="
            + m_heightModel.getIntValue() + ", pointsize=" + m_pointSizeModel.getIntValue() + ", bg=\""
            + m_bgModel.getStringValue() + "\"" + ", res=" + m_resolutionModel.getStringValue() + ");\n" + m_viewCmd
            + "\ndev.off();";
    }

    /**
     * After execution of the R code and image instance is created which can be displayed by the nodes view.
     *
     * {@inheritDoc}
     */
    @Override
    protected final BufferedDataTable[] postprocessDataTable(final BufferedDataTable[] outData,
        final ExecutionContext exec) throws CanceledExecutionException, Exception {
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        super.execute(inData, exec);
        // create image after execution.
        final FileInputStream fis = new FileInputStream(new File(m_filename));
        final PNGImageContent content = new PNGImageContent(fis);
        fis.close();
        m_resultImage = content.getImage();
        return new PortObject[]{new ImagePortObject(content, OUT_SPEC)};
    }

    /**
     * Before execution of the R code the column filtering is done.
     *
     * {@inheritDoc}
     */
    @Override
    protected final PortObject[] preprocessDataTable(final PortObject[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, Exception {
        m_filename = FileUtil.createTempDir("R_").getAbsolutePath().replace('\\', '/') + "/" + "R-View-"
            + System.identityHashCode(inData) + ".png";

        return inData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        checkRExecutable();
        return new PortObjectSpec[]{OUT_SPEC};
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
        m_viewCmd = RDialogPanel.getExpressionFrom(settings);
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_heightModel.saveSettingsTo(settings);
        m_widthModel.saveSettingsTo(settings);
        m_resolutionModel.saveSettingsTo(settings);
        m_pointSizeModel.saveSettingsTo(settings);
        m_bgModel.saveSettingsTo(settings);
        RDialogPanel.setExpressionTo(settings, m_viewCmd);
        m_viewType.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);

        final String viewCmd = RDialogPanel.getExpressionFrom(settings);

        // if command not valid throw exception
        if ((viewCmd == null) || (viewCmd.length() < 1)) {
            throw new InvalidSettingsException("R View command is empty!");
        }

        m_heightModel.validateSettings(settings);
        m_widthModel.validateSettings(settings);
        // new with 2.3.1: no validation possible
        // m_resolutionModel.validateSettings(settings);
        m_pointSizeModel.validateSettings(settings);
        m_bgModel.validateSettings(settings);

        // validate background color code
        final String colorCode =
            ((SettingsModelString)m_bgModel.createCloneWithValidatedValue(settings)).getStringValue();
        if (!colorCode.matches("^#[0-9aAbBcCdDeEfF]{6}")) {
            throw new InvalidSettingsException("Specified color code \"" + colorCode + "\" is not valid!");
        }
    }

    /**
     * The saved image is loaded.
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);

        final File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
        if (file.exists() && file.canRead()) {
            final File pngFile =
                File.createTempFile(INTERNAL_FILE_NAME, ".png", new File(KNIMEConstants.getKNIMETempDir()));
            FileUtil.copy(file, pngFile);
            final InputStream is = new FileInputStream(pngFile);
            m_resultImage = new PNGImageContent(is).getImage();
            is.close();
        }
    }

    /**
     * The created image is saved.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);

        final File imgFile = new File(m_filename);
        if (imgFile.exists() && imgFile.canWrite()) {
            final File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
            FileUtil.copy(imgFile, file);
        }
    }
}
