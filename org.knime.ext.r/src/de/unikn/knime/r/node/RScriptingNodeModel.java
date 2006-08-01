/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
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
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.r.node;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * Executes R commands locally, the entire data is transfered to the R Server 
 * in the execute.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public class RScriptingNodeModel extends RNodeModel {
    private DataTableSpec m_spec = null;

    protected RScriptingNodeModel() {
        super(1, 0);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        m_spec = inData[0].getDataTableSpec();
        // create first a connection object
        RConnection.sendData(getRconnection(), inData[0], exec);
        // nothing
        return new BufferedDataTable[0];
    }
    
    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_spec = null;
    }
    
    /**
     * @return Input spec used in view show all columns.
     */
    final DataTableSpec getDataTableSpec() {
        assert m_spec != null;
        return m_spec;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #configure(de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkRconnection();
        m_spec = inSpecs[0];
        return new DataTableSpec[0];
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
    }
    
    /**
     * @see de.unikn.knime.core.node.NodeModel#loadInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#saveInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

}
