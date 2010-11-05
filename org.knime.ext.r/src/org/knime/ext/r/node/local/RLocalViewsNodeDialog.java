/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2010
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
 * History
 *   18.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The dialog of the <code>RLocalViewsNodeDialog</code> which provides a
 * drop down menu containing a set of names of R plots and a multi line text
 * field to specify R code for the usage of plots. When a certain R plot
 * is specified using the drop down menu, a dummy R code template is
 * shown up in the multi line text field. Additionally a column selection panel
 * is provided to specify columns to use and import into R.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeDialog extends RLocalNodeDialogPane {

    private static final String TAB_TITLE = "R View Command";

    private final RViewsDialogPanel m_viewScriptPanel;
    private final RViewsPngDialogPanel m_viewPngPanel;

    /**
     * Creates new instance of <code>RLocalViewsNodeDialog</code>.
     */
    public RLocalViewsNodeDialog() {
        super();
        m_viewScriptPanel = new RViewsDialogPanel();
        addTabAt(0, TAB_TITLE, m_viewScriptPanel);
        setSelected(TAB_TITLE);

        m_viewPngPanel = new RViewsPngDialogPanel();
        addTabAt(1, RViewsPngDialogPanel.TAB_PNG_TITLE, m_viewPngPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        Map<String, FlowVariable> flowMap = getAvailableFlowVariables();
        m_viewScriptPanel.loadSettings(settings, specs, flowMap);
        m_viewPngPanel.loadSettings(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        m_viewScriptPanel.saveSettings(settings);
        m_viewPngPanel.saveSettings(settings);
    }
}
