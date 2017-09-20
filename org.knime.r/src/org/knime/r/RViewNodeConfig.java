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
package org.knime.r;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.util.FileUtil;

import com.sun.jna.Platform;

/**
 * Configurations for R nodes with image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeConfig extends RSnippetNodeConfig {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RViewNodeConfig.class);

    static final String[] IMAGE_TYPES = new String[]{"PNG", "SVG"};

    private final PortType m_inPortType;

    private File m_imageFile;

    private RViewNodeSettings m_settings;

    public RViewNodeConfig(final PortType inPortType) {
        m_inPortType = inPortType;
    }

    @Override
    public Collection<PortType> getInPortTypes() {
        return Collections.singleton(m_inPortType);
    }

    @Override
    protected Collection<PortType> getOutPortTypes() {
        return Collections.singleton(ImagePortObject.TYPE);
    }

    @Override
    protected String getScriptPrefix() {
        final StringBuilder prefix = new StringBuilder();

        String bitmapType = "";
        if (Platform.isMac()) {
            bitmapType = ",bitmapType='cairo'";
            prefix.append("library('Cairo');");
        }

        // Image type: SVG or PNG
        final String imgType = m_settings.getImageType().toLowerCase();

        prefix.append("options(device = '" + imgType + "'" + bitmapType + ")").append("\n");

        prefix.append(imgType.toLowerCase() + "(");

        final File imageFile = getImageFile();
        prefix.append("\"" + imageFile.getAbsolutePath().replace('\\', '/') + "\",");

        int svgResolution = 1;
        if(imgType.equals("svg")) {
            svgResolution = 300;
            try {
                svgResolution = Integer.parseInt(m_settings.getImageResolution());
            } catch (NumberFormatException e) {
                // Either NA or invalid for SVG resolution, just use default 300.
            }
        }

        /* For svg() height is measured in inch, which is why we divide by the dpi
         * specified by the resolution. With png the resolution is set to 1 so that
         * the division has no effect.
         */
        final int width = m_settings.getImageWidth() / svgResolution;
        final int height = m_settings.getImageHeight() / svgResolution;

        prefix.append("width=" + width + ",");
        prefix.append("height=" + height + ",");
        prefix.append("pointsize=" + m_settings.getTextPointSize() + ",");
        prefix.append("bg=\"" + m_settings.getImageBackgroundColor() + "\",");

        if(imgType.equals("png")) {
            prefix.append("res=" + m_settings.getImageResolution() + ")");
        } else {
            prefix.append(")");
        }

        return prefix.toString();
    }

    @Override
    protected String getScriptSuffix() {
        return "dev.off()";
    }

    /**
     * Non-null image file to use for this current node. Lazy-initialized to temp location.
     *
     * @return File of the created image
     */
    public File getImageFile() {
        if (m_imageFile == null) {
            try {
                m_imageFile = FileUtil.createTempFile("R-view-", "." + m_settings.getImageType().toLowerCase());
            } catch (final IOException e) {
                LOGGER.error("Cannot create temporary file.", e);
                throw new RuntimeException(e);
            }
            m_imageFile.deleteOnExit();
        }
        return m_imageFile;
    }

    /**
     * @param settings Settings of the node that should be configured with this config
     */
    public void setSettings(final RViewNodeSettings settings) {
        m_settings = settings;
    }

    @Override
    public String getDefaultScript() {
        if (BufferedDataTable.TYPE.equals(m_inPortType)) {
            return "plot(knime.in)\n";
        } else {
            return "plot(iris)\n";
        }
    }
}
