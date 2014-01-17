/*
 * ------------------------------------------------------------------
 * Copyright by 
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node.local;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
@SuppressWarnings("serial")
public class RViewsPngDialogPanel extends JLabel {

    /** Default tab title for this panel. */
    public static final String TAB_PNG_TITLE = "PNG Settings";

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance
     * containing the height of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createHeightModel() {
        return new SettingsModelIntegerBounded("R-png-height",
                RLocalViewsNodeModel2.IMG_DEF_SIZE,
                RLocalViewsNodeModel2.IMG_MIN_HEIGHT,
                RLocalViewsNodeModel2.IMG_MAX_HEIGHT);
    }

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance
     * containing the width of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createWidthModel() {
        return new SettingsModelIntegerBounded("R-png-width",
                RLocalViewsNodeModel2.IMG_DEF_SIZE,
                RLocalViewsNodeModel2.IMG_MIN_WIDTH,
                RLocalViewsNodeModel2.IMG_MAX_WIDTH);
    }

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance
     * containing the point size of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createPointSizeModel() {
        return new SettingsModelIntegerBounded("R-png-pointSize", 12, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return a <code>SettingsModelString</code> instance
     * containing the resolution of the png image to create by R.
     */
    public static final SettingsModelString createResolutionModel() {
        return new SettingsModelString("R-png-resolution", "NA");
    }

    /**
     * @return a <code>SettingsModelString</code> instance
     * containing the background color of the png image to create by R.
     */
    public static final SettingsModelString createBgModel() {
        return new SettingsModelString("R-bg-col", "#FFFFFF");
    }

    private final DialogComponentNumberEdit m_heightComp;
    private final SettingsModelIntegerBounded m_heightModel;

    private final DialogComponentNumberEdit m_widthComp;
    private final SettingsModelIntegerBounded m_widthModel;

    private final DialogComponentNumber m_pointSizeComp;
    private final SettingsModelIntegerBounded m_pointSizeModel;

    private final DialogComponentString m_bgComp;
    private final SettingsModelString m_bgModel;

    private final DialogComponentString m_resolutionComp;
    private final SettingsModelString m_resolutionModel;

    /**
     * Creates new instance of <code>RViewsPngDialogPanel</code> which provides
     * default dialog components to specify png options used by R to create
     * the png image.
     */
    public RViewsPngDialogPanel() {
        super();
        super.setLayout(new BorderLayout());

        m_heightModel = createHeightModel();
        m_heightComp = new DialogComponentNumberEdit(m_heightModel,
                "Height: ");

        m_widthModel = createWidthModel();
        m_widthComp = new DialogComponentNumberEdit(m_widthModel,
                "Width: ");

        // Resolution (dpi)
        m_resolutionModel = createResolutionModel();
        m_resolutionComp = new DialogComponentString(m_resolutionModel,
                "Resolution (dpi): ");
        m_resolutionComp.setToolTipText("Resolution in dots per inch; "
                + "default value is NA (72 dpi)");

        // Image size
        JPanel size = new JPanel(new BorderLayout());
        size.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Image size "));
        size.add(m_widthComp.getComponentPanel(), BorderLayout.WEST);
        size.add(m_heightComp.getComponentPanel(), BorderLayout.CENTER);
        size.add(m_resolutionComp.getComponentPanel(), BorderLayout.EAST);

        // Point size
        m_pointSizeModel = createPointSizeModel();
        m_pointSizeComp = new DialogComponentNumber(m_pointSizeModel,
                "Point size: ", 1);

        // Background color
        m_bgModel = createBgModel();
        m_bgComp = new DialogComponentString(m_bgModel , "Background color: ");

        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Appearance "));
        upperPanel.add(m_pointSizeComp.getComponentPanel(), BorderLayout.NORTH);
        upperPanel.add(m_bgComp.getComponentPanel(), BorderLayout.CENTER);

        // add all components
        add(size, BorderLayout.NORTH);
        add(upperPanel, BorderLayout.CENTER);
    }

    /**
     * Loads settings into dialog components.
     * @param settings The settings to load.
     * @param specs The specs of the input data table.
     * @throws NotConfigurableException If components could not be configured
     * and settings not be set.
     */
    public void loadSettings(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_heightComp.loadSettingsFrom(settings, specs);
        m_widthComp.loadSettingsFrom(settings, specs);
        m_pointSizeComp.loadSettingsFrom(settings, specs);
        m_bgComp.loadSettingsFrom(settings, specs);
        m_resolutionComp.loadSettingsFrom(settings, specs);
    }

    /**
     * Saves settings set in the dialog components into the settings instance.
     * @param settings The settings instance to save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        m_heightComp.saveSettingsTo(settings);
        m_widthComp.saveSettingsTo(settings);
        m_pointSizeComp.saveSettingsTo(settings);
        m_bgComp.saveSettingsTo(settings);
        m_resolutionComp.saveSettingsTo(settings);
    }
}
