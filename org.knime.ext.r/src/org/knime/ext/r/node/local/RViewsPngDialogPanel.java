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
     * @return a <code>SettingsModelIntegerBounded</code> instance containing the height of the png image to create by
     *         R.
     */
    public static final SettingsModelIntegerBounded createHeightModel() {
        return new SettingsModelIntegerBounded("R-png-height", RLocalViewsNodeModel2.IMG_DEF_SIZE,
            RLocalViewsNodeModel2.IMG_MIN_HEIGHT, RLocalViewsNodeModel2.IMG_MAX_HEIGHT);
    }

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance containing the width of the png image to create by R.
     */
    public static final SettingsModelIntegerBounded createWidthModel() {
        return new SettingsModelIntegerBounded("R-png-width", RLocalViewsNodeModel2.IMG_DEF_SIZE,
            RLocalViewsNodeModel2.IMG_MIN_WIDTH, RLocalViewsNodeModel2.IMG_MAX_WIDTH);
    }

    /**
     * @return a <code>SettingsModelIntegerBounded</code> instance containing the point size of the png image to create
     *         by R.
     */
    public static final SettingsModelIntegerBounded createPointSizeModel() {
        return new SettingsModelIntegerBounded("R-png-pointSize", 12, 1, Integer.MAX_VALUE);
    }

    /**
     * @return a <code>SettingsModelString</code> instance containing the resolution of the png image to create by R.
     */
    public static final SettingsModelString createResolutionModel() {
        return new SettingsModelString("R-png-resolution", "NA");
    }

    /**
     * @return a <code>SettingsModelString</code> instance containing the background color of the png image to create by
     *         R.
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
     * Creates new instance of <code>RViewsPngDialogPanel</code> which provides default dialog components to specify png
     * options used by R to create the png image.
     */
    public RViewsPngDialogPanel() {
        super();
        super.setLayout(new BorderLayout());

        m_heightModel = createHeightModel();
        m_heightComp = new DialogComponentNumberEdit(m_heightModel, "Height: ");

        m_widthModel = createWidthModel();
        m_widthComp = new DialogComponentNumberEdit(m_widthModel, "Width: ");

        // Resolution (dpi)
        m_resolutionModel = createResolutionModel();
        m_resolutionComp = new DialogComponentString(m_resolutionModel, "Resolution (dpi): ");
        m_resolutionComp.setToolTipText("Resolution in dots per inch; " + "default value is NA (72 dpi)");

        // Image size
        final JPanel size = new JPanel(new BorderLayout());
        size.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Image size "));
        size.add(m_widthComp.getComponentPanel(), BorderLayout.WEST);
        size.add(m_heightComp.getComponentPanel(), BorderLayout.CENTER);
        size.add(m_resolutionComp.getComponentPanel(), BorderLayout.EAST);

        // Point size
        m_pointSizeModel = createPointSizeModel();
        m_pointSizeComp = new DialogComponentNumber(m_pointSizeModel, "Point size: ", 1);

        // Background color
        m_bgModel = createBgModel();
        m_bgComp = new DialogComponentString(m_bgModel, "Background color: ");

        final JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Appearance "));
        upperPanel.add(m_pointSizeComp.getComponentPanel(), BorderLayout.NORTH);
        upperPanel.add(m_bgComp.getComponentPanel(), BorderLayout.CENTER);

        // add all components
        add(size, BorderLayout.NORTH);
        add(upperPanel, BorderLayout.CENTER);
    }

    /**
     * Loads settings into dialog components.
     * 
     * @param settings The settings to load.
     * @param specs The specs of the input data table.
     * @throws NotConfigurableException If components could not be configured and settings not be set.
     */
    public void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_heightComp.loadSettingsFrom(settings, specs);
        m_widthComp.loadSettingsFrom(settings, specs);
        m_pointSizeComp.loadSettingsFrom(settings, specs);
        m_bgComp.loadSettingsFrom(settings, specs);
        m_resolutionComp.loadSettingsFrom(settings, specs);
    }

    /**
     * Saves settings set in the dialog components into the settings instance.
     * 
     * @param settings The settings instance to save settings to.
     * @throws InvalidSettingsException If invalid settings have been set.
     */
    public void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_heightComp.saveSettingsTo(settings);
        m_widthComp.saveSettingsTo(settings);
        m_pointSizeComp.saveSettingsTo(settings);
        m_bgComp.saveSettingsTo(settings);
        m_resolutionComp.saveSettingsTo(settings);
    }
}
