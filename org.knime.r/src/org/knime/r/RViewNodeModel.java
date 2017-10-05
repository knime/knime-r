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
package org.knime.r;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.base.data.xml.SvgCell;
import org.knime.base.data.xml.SvgImageContent;
import org.knime.core.data.DataType;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 * The <code>RViewNodeModel</code> is the node model for the r-nodes with an image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeModel extends RSnippetNodeModel {

    private final RViewNodeSettings m_settings;

    private ImageContent m_resultImage;

    private static final String INTERNAL_FILE_NAME = "Rplot";

    /**
     * Constructor
     *
     * @param config Used to configure the generic R base node.
     */
    public RViewNodeModel(final RViewNodeConfig config) {
        super(config);
        m_settings = new RViewNodeSettings(getSettings());
        getConfig().setSettings(m_settings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final String imgType = m_settings.getImageType();
        CheckUtils.checkSetting(ArrayUtils.contains(RViewNodeConfig.IMAGE_TYPES, imgType),
            "Unknown image type \"%s\"", imgType);
        return new PortObjectSpec[]{createOutSpec()};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        super.execute(inData, exec);
        return postExecuteInternal();

    }

    private ImagePortObjectSpec createOutSpec() {
        final DataType type = (m_settings.getImageType().equals("PNG")) ? PNGImageContent.TYPE : SvgCell.TYPE;
        return new ImagePortObjectSpec(type);
    }

    private PortObject[] postExecuteInternal() throws IOException {
        if (getConfig().getImageFile().length() > 0) {
            // create image after execution.
            try (FileInputStream fis = new FileInputStream(getConfig().getImageFile())) {
                if (m_settings.getImageType().equals("PNG")) {
                    m_resultImage = new PNGImageContent(fis);
                } else if (m_settings.getImageType().equals("SVG")) {
                    m_resultImage = new SvgImageContent(fis);
                }
            }
            return new PortObject[]{new ImagePortObject(m_resultImage, createOutSpec())};
        } else {
            throw new IOException("No Image was created by the R-Script");
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final RViewNodeSettings s = new RViewNodeSettings();
        s.loadSettings(settings);

        // validate background color code
        final String colorCode = s.getImageBackgroundColor();
        if (!colorCode.matches("^#[0-9aAbBcCdDeEfF]{6}")) {
            throw new InvalidSettingsException("Specified color code \"" + colorCode + "\" is not valid!");
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
        getRSnippet().getSettings().loadSettings(m_settings.getRSettings());
    }

    /**
     * {@inheritDoc}
     *
     * The saved image is loaded.
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.loadInternals(nodeInternDir, exec);

        final String imgType = m_settings.getImageType();
        final File file = new File(nodeInternDir, INTERNAL_FILE_NAME + "." + imgType.toLowerCase());
        if (file.exists() && file.canRead()) {
            final File imgFile = getConfig().getImageFile();
            FileUtil.copy(file, imgFile);

            if (imgType.equals("PNG")) {
                try (InputStream is = new FileInputStream(imgFile)) {
                    m_resultImage = new PNGImageContent(is);
                }
            } else if (imgType.equals("SVG")) {
                try (InputStream is = new FileInputStream(imgFile)) {
                    m_resultImage = new SvgImageContent(is);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * The created image is saved.
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        super.saveInternals(nodeInternDir, exec);

        if (m_resultImage != null) {
            final File file =
                new File(nodeInternDir, INTERNAL_FILE_NAME + "." + m_settings.getImageType().toLowerCase());
            FileUtil.copy(getConfig().getImageFile(), file);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultImage = null;
        // clear image file contents
        if (getConfig().getImageFile() != null) {
            try {
                try (FileOutputStream erasor = new FileOutputStream(getConfig().getImageFile())) {
                    erasor.write(ArrayUtils.EMPTY_BYTE_ARRAY);
                }
            } catch (final FileNotFoundException e) {
                getLogger().error("Temporary file is removed.", e);
            } catch (final IOException e) {
                getLogger().error("Cannot write temporary file.", e);
            }
        }
    }

    private RViewNodeConfig getConfig() {
        return (RViewNodeConfig)getRSnippetNodeConfig();
    }

    /**
     * @return The image that was generated by the R code
     */
    public ImageContent getResultImage() {
        return m_resultImage;
    }
}
