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
 */
package org.knime.ext.r.node.local;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.r.node.RDialogPanel;


/**
 * A dialog containing a multi line text field to specify the R code to run.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalScriptingNodeDialogPane extends RLocalNodeDialogPane {

    private final RDialogPanel m_dialogPanel;

    /**
     * Constructor which creates a new instance of
     * <code>RLocalScriptingNodeDialogPane</code>. A dialog component is added
     * which allows users to enter R code.
     */
    public RLocalScriptingNodeDialogPane() {
        super();
        m_dialogPanel = new RDialogPanel();
        addTabAt(0, "R Command", m_dialogPanel);
        setSelected("R Command");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        Map<String, FlowVariable> flowMap = getAvailableFlowVariables();
        m_dialogPanel.loadSettingsFrom(settings, specs, flowMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        m_dialogPanel.saveSettingsTo(settings);
    }
}
