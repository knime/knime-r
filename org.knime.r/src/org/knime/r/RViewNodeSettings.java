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

    private static final String R_SETTINGS = "R settings";

    /** Image width. */
    private int m_imgWidth;

    /** Image height. */
    private int m_imgHeight;

    /** Image resolution. */
    private String m_imgResolution;

    /** Image background color. */
    private String m_imgBackgroundColor;

    /** Text point size. */
    private int m_textPointSize;

    /** R settings */
    private RSnippetSettings m_rSettings;

    public RViewNodeSettings() {
        this(new RSnippetSettings());
    }

    public RViewNodeSettings(final RSnippetSettings rSnippetSettings) {
        m_imgWidth = 640;
        m_imgHeight = 640;
        m_imgResolution = "NA";
        m_imgBackgroundColor = "#ffffff";
        m_textPointSize = 12;
        m_rSettings = rSnippetSettings;
    }

    public static ConfigRO extractRSettings(final ConfigRO config) {
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
    }

    /**
     * @return the m_imgWidth
     */
    public int getImageWidth() {
        return m_imgWidth;
    }

    /**
     * @param m_imgWidth the m_imgWidth to set
     */
    public void setImageWidth(final int imgWidth) {
        this.m_imgWidth = imgWidth;
    }

    /**
     * @return the m_imgHeight
     */
    public int getImageHeight() {
        return m_imgHeight;
    }

    /**
     * @param m_imgHeight the m_imgHeight to set
     */
    public void setImageHeight(final int imgHeight) {
        this.m_imgHeight = imgHeight;
    }

    /**
     * @return the m_imgResolution
     */
    public String getImageResolution() {
        return m_imgResolution;
    }

    /**
     * @param m_imgResolution the m_imgResolution to set
     */
    public void setImageResolution(final String imgResolution) {
        this.m_imgResolution = imgResolution;
    }

    /**
     * @return the m_imgBackgroundColor
     */
    public String getImageBackgroundColor() {
        return m_imgBackgroundColor;
    }

    /**
     * @param m_imgBackgroundColor the m_imgBackgroundColor to set
     */
    public void setImageBackgroundColor(final String imgBackgroundColor) {
        this.m_imgBackgroundColor = imgBackgroundColor;
    }

    /**
     * @return the m_textPointSize
     */
    public int getTextPointSize() {
        return m_textPointSize;
    }

    /**
     * @param m_textPointSize the m_textPointSize to set
     */
    public void setTextPointSize(final int textPointSize) {
        this.m_textPointSize = textPointSize;
    }

    /**
     * @return the m_rSettings
     */
    public RSnippetSettings getRSettings() {
        return m_rSettings;
    }

    /**
     * @param m_rSettings the m_rSettings to set
     */
    public void setRSettings(final RSnippetSettings rSettings) {
        this.m_rSettings = rSettings;
    }

}
