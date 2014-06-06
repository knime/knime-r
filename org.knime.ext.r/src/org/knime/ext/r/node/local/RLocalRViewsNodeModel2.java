/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.core.data.image.png.PNGImageContent;
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
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.preferences.RPreferenceProvider;

/**
 * The <code>RLocalRViewsNodeModel</code> provides functionality to create
 * a R script with user defined R code calling R plots, run it and display
 * the generated plot in the nodes view.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
public class RLocalRViewsNodeModel2 extends RAbstractLocalNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RLocalRViewsNodeModel2.class);

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
    private static final ImagePortObjectSpec OUT_SPEC =
        new ImagePortObjectSpec(PNGImageContent.TYPE);

    private final SettingsModelIntegerBounded m_heightModel =
        RViewsPngDialogPanel.createHeightModel();

    private final SettingsModelIntegerBounded m_widthModel =
        RViewsPngDialogPanel.createWidthModel();

    private final SettingsModelString m_resolutionModel =
        RViewsPngDialogPanel.createResolutionModel();

    private final SettingsModelIntegerBounded m_pointSizeModel =
        RViewsPngDialogPanel.createPointSizeModel();

    private final SettingsModelString m_bgModel =
        RViewsPngDialogPanel.createBgModel();

    private final SettingsModelString m_viewType =
        RViewsDialogPanel.createViewSettingsModel();

    private Image m_resultImage;

    private String m_filename;

    private String m_viewCmd = RViewScriptingConstants.getDefaultExpressionCommand();

    /**
     * Creates new instance of <code>RLocalRViewsNodeModel</code> with one data in port and no data out port.
     * @param pref provider for R executable
     */
    public RLocalRViewsNodeModel2(final RPreferenceProvider pref) {
        super(new PortType[]{RPortObject.TYPE}, new PortType[]{ImagePortObject.TYPE}, pref);
        m_resultImage = null;
    }

    /**
     * @return result image for the view, only available after successful
     *         execution of the node model.
     */
    Image getResultImage() {
        return m_resultImage;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inPorts, final ExecutionContext exec) throws Exception {

        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<String>());
        setFailedExternalOutput(new LinkedList<String>());

        File rCommandFile = null;
        File rOutFile = null;

        try {
            // execute R cmd
            StringBuilder completeCmd = new StringBuilder();
            completeCmd.append(getSetWorkingDirCmd());

            // load model
            File fileR = ((RPortObject)inPorts[0]).getFile();
            completeCmd.append(LOAD_MODEL_CMD_PREFIX);
            completeCmd.append(fileR.getAbsolutePath().replace('\\', '/'));
            completeCmd.append(LOAD_MODEL_CMD_SUFFIX);

            // create tmp file with image content
            m_filename = FileUtil.createTempDir("R_").getAbsolutePath().replace('\\', '/')
                + "/" + "R-View-" + System.identityHashCode(inPorts) + ".png";

            // result data
            final String command = "png(\"" + m_filename + "\""
                + ", width=" + m_widthModel.getIntValue()
                + ", height=" + m_heightModel.getIntValue()
                + ", pointsize=" + m_pointSizeModel.getIntValue()
                + ", bg=\"" + m_bgModel.getStringValue() + "\""
                + ", res=" + m_resolutionModel.getStringValue() + ");\n"
                + m_viewCmd + "\ndev.off();";
            completeCmd.append(resolveVariablesInScript(command));
            completeCmd.append("\n");

            // write R command
            String rCmd = completeCmd.toString();
            LOGGER.debug("R Command: \n" + rCmd);
            rCommandFile = writeRcommandFile(rCmd);
            rOutFile = new File(rCommandFile.getAbsolutePath() + ".Rout");

            // create shell command
            StringBuilder shellCmd = new StringBuilder();

            final String rBinaryFile = getRBinaryPathAndArguments();
            shellCmd.append(rBinaryFile);
            shellCmd.append(" " + rCommandFile.getName());
            shellCmd.append(" " + rOutFile.getName());


            // execute shell command
            String shcmd = shellCmd.toString();
            LOGGER.debug("Shell command: \n" + shcmd);

            CommandExecution cmdExec = new CommandExecution(shcmd);
            cmdExec.addObserver(this);
            cmdExec.setExecutionDir(rCommandFile.getParentFile());
            int exitVal = cmdExec.execute(exec);

            setExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
            setExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));

            String rErr = "";

            if (exitVal != 0) {
                // before we return, we save the output in the failing list
                synchronized (cmdExec) {
                    setFailedExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));
                }
            }
            synchronized (cmdExec) {

                // save error description of the Rout file to the ErrorOut
                LinkedList<String> list = new LinkedList<String>(cmdExec.getStdErr());

                list.add("#############################################");
                list.add("#");
                list.add("# Content of .Rout file: ");
                list.add("#");
                list.add("#############################################");
                list.add(" ");
                BufferedReader bfr = new BufferedReader(new FileReader(rOutFile));
                String line;
                while ((line = bfr.readLine()) != null) {
                    list.add(line);
                }
                bfr.close();

                // use row before last as R error.
                int index = list.size() - 2;
                if (index >= 0) {
                    rErr = list.get(index);
                }

                if (exitVal != 0) {
                    setFailedExternalErrorOutput(list);
                    LOGGER.debug("Execution of R Script failed with exit code: " + exitVal);
                    throw new IllegalStateException("Execution of R script failed: " + rErr);
                } else {
                    setExternalOutput(list);
                }
            }

            // create image after execution

            FileInputStream fis = new FileInputStream(new File(m_filename));
            PNGImageContent content = new PNGImageContent(fis);
            fis.close();
            m_resultImage = content.getImage();
            // return image
            return new PortObject[] {new ImagePortObject(content, OUT_SPEC)};
        } finally {
            // delete all temp files
            deleteFile(rCommandFile);
            deleteFile(rOutFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        super.reset();
        m_filename = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        checkRExecutable();
        return new PortObjectSpec[] {OUT_SPEC};
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
        m_resolutionModel.loadSettingsFrom(settings);
        m_pointSizeModel.loadSettingsFrom(settings);
        m_bgModel.loadSettingsFrom(settings);
        m_viewCmd = RDialogPanel.getExpressionFrom(settings);
        m_viewType.loadSettingsFrom(settings);
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);

        String viewCmd = RDialogPanel.getExpressionFrom(settings);

        // if command not valid throw exception
        if (viewCmd == null || viewCmd.length() < 1) {
            throw new InvalidSettingsException("R View command is empty!");
        }

        m_heightModel.validateSettings(settings);
        m_widthModel.validateSettings(settings);
        m_resolutionModel.validateSettings(settings);
        m_pointSizeModel.validateSettings(settings);
        m_bgModel.validateSettings(settings);

        // validate background color code
        String colorCode = ((SettingsModelString)m_bgModel.createCloneWithValidatedValue(settings)).getStringValue();
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
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);

        File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
        if (file.exists() && file.canRead()) {
            File pngFile = File.createTempFile(INTERNAL_FILE_NAME, ".png", new File(KNIMEConstants.getKNIMETempDir()));
            FileUtil.copy(file, pngFile);
            InputStream is = new FileInputStream(pngFile);
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);

        File imgFile = new File(m_filename);
        if (imgFile.exists() && imgFile.canWrite()) {
            File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
            FileUtil.copy(imgFile, file);
        }
    }
}
