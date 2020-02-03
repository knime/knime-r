/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;

/**
 * The settings for the R-nodes with an image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeSettings {

    private static final String IMAGE_WIDTH = "Image width";

    private static final String IMAGE_HEIGHT = "Image height";

    private static final String IMAGE_RESOLUTION = "Image resolution";

    private static final String IMAGE_BACKGROUND_COLOR = "Image background color";

    private static final String TEXT_POINT_SIZE = "Text point size";

    static final String R_SETTINGS = "R settings";

    private static final String IMAGE_TYPE = "Image type";

    /** Image width. */
    private int m_imgWidth = 640;

    /** Image height. */
    private int m_imgHeight = 640;

    /** Image resolution. */
    private String m_imgResolution = "NA";

    /** Image background color. */
    private String m_imgBackgroundColor = "#ffffff";

    /** Text point size. */
    private int m_textPointSize = 12;

    /** R settings */
    private RSnippetSettings m_rSettings;

    /** Image type ("PNG"/"SVG") (Since 3.5) */
    private String m_imgType = "PNG";

    /**
     * Constructor
     */
    public RViewNodeSettings() {
        this(new RSnippetSettings());
    }

    /**
     * Constructor
     *
     * @param rSnippetSettings Settings for the base (generic RSnippet node).
     */
    public RViewNodeSettings(final RSnippetSettings rSnippetSettings) {
        m_rSettings = rSnippetSettings;
    }

    /**
     * Load only the settings of the generic RSnippet base node from config.
     *
     * @param config The config to load from.
     * @return Config for the RSnippet node, stored at the {@link RViewNodeSettings#R_SETTINGS} key.
     */
    static ConfigRO extractRSettings(final ConfigRO config) {
        if (config.containsKey(R_SETTINGS)) {
            try {
                return config.getConfig(R_SETTINGS);
            } catch (final InvalidSettingsException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Saves current parameters to settings object.
     *
     * @param settings To save to.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addInt(IMAGE_WIDTH, m_imgWidth);
        settings.addInt(IMAGE_HEIGHT, m_imgHeight);
        settings.addString(IMAGE_RESOLUTION, m_imgResolution);
        settings.addString(IMAGE_BACKGROUND_COLOR, m_imgBackgroundColor);
        settings.addInt(TEXT_POINT_SIZE, m_textPointSize);
        m_rSettings.saveSettings(settings.addConfig(R_SETTINGS));

        // Since 3.5
        settings.addString(IMAGE_TYPE, m_imgType);
    }

    /**
     * Loads parameters in NodeModel.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        if (settings.containsKey(IMAGE_WIDTH)) {
            m_imgWidth = settings.getInt(IMAGE_WIDTH);
            m_imgHeight = settings.getInt(IMAGE_HEIGHT);
            m_imgResolution = settings.getString(IMAGE_RESOLUTION);
            m_imgBackgroundColor = settings.getString(IMAGE_BACKGROUND_COLOR);
            m_textPointSize = settings.getInt(TEXT_POINT_SIZE);
            m_rSettings.loadSettings(settings.getConfig(R_SETTINGS));

            // Since 3.5
            m_imgType = settings.getString(IMAGE_TYPE, "PNG");
        } else {
            // Support just R Settings
            m_rSettings.loadSettings(settings);
        }
    }

    /**
     * Loads parameters in Dialog.
     *
     * @param settings To load from.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_imgWidth = settings.getInt(IMAGE_WIDTH, 640);
        m_imgHeight = settings.getInt(IMAGE_HEIGHT, 640);
        m_imgResolution = settings.getString(IMAGE_RESOLUTION, "NA");
        m_imgBackgroundColor = settings.getString(IMAGE_BACKGROUND_COLOR, "#ffffff");
        m_textPointSize = settings.getInt(TEXT_POINT_SIZE, 12);
        if (settings.containsKey(R_SETTINGS)) {
            try {
                m_rSettings.loadSettingsForDialog(settings.getConfig(R_SETTINGS));
            } catch (final InvalidSettingsException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        } else {
            m_rSettings = new RSnippetSettings();
        }

        // Since 3.5
        m_imgType = settings.getString(IMAGE_TYPE, "PNG");
    }

    /**
     * @return the width for the output image
     */
    public int getImageWidth() {
        return m_imgWidth;
    }

    /**
     * Set width for the output image
     *
     * @param imgWidth the m_imgWidth to set
     */
    public void setImageWidth(final int imgWidth) {
        this.m_imgWidth = imgWidth;
    }

    /**
     * @return the height for the output image
     */
    public int getImageHeight() {
        return m_imgHeight;
    }

    /**
     * Set height for the output image
     *
     * @param imgHeight the height
     */
    public void setImageHeight(final int imgHeight) {
        m_imgHeight = imgHeight;
    }

    /**
     * @return the R expression for the output image resolution
     */
    public String getImageResolution() {
        return m_imgResolution;
    }

    /**
     * Set resolution for the output image.
     *
     * @param imgResolution the R expression for the resolution (can be "NA")
     */
    public void setImageResolution(final String imgResolution) {
        m_imgResolution = imgResolution;
    }

    /**
     * @return the m_imgBackgroundColor
     */
    public String getImageBackgroundColor() {
        return m_imgBackgroundColor;
    }

    /**
     * @param imgBackgroundColor the background color for output images
     */
    public void setImageBackgroundColor(final String imgBackgroundColor) {
        m_imgBackgroundColor = imgBackgroundColor;
    }

    /**
     * @return the text size in output plots (in pt)
     */
    public int getTextPointSize() {
        return m_textPointSize;
    }

    /**
     * @param textPointSize the m_textPointSize to set
     */
    public void setTextPointSize(final int textPointSize) {
        m_textPointSize = textPointSize;
    }

    /**
     * @return the m_rSettings
     */
    public RSnippetSettings getRSettings() {
        return m_rSettings;
    }

    /**
     * @param rSettings The RSnippet settings (settings of the generic RSnippet node base)
     */
    public void setRSettings(final RSnippetSettings rSettings) {
        m_rSettings = rSettings;
    }

    /**
     * @return Type for the output image ("PNG" or "SVG")
     */
    public String getImageType() {
        return m_imgType;
    }

    /**
     * Set the type for the output image ("PNG" or "SVG").
     *
     * @param imgType Image type for the output
     */
    public void setImageType(final String imgType) {
        m_imgType = imgType;
    }
}
