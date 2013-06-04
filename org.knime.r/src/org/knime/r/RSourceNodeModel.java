/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2013
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
 *   17.09.2007 (thiel): created
 */
package org.knime.r;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The <code>RSourceNodeModel</code> provides functionality to create
 * a R script with user defined R code and run it.
 *
 * @author Heiko Hofer
 */
public class RSourceNodeModel extends ExtToolOutputNodeModel {
    private RSnippetSettings m_settings;
    private RSnippet m_snippet;
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
        "R Snippet");
    
    /**
     * Creates new instance of <code>RSourceNodeModel</code> with one
     * data in and data one out port.
     * @param pref R preference provider
     */
    public RSourceNodeModel() {
        super(new PortType[]{}, new PortType[]{BufferedDataTable.TYPE});
        m_settings = new RSnippetSettings();
        m_snippet = new RSnippet();
        m_snippet.attachLogger(LOGGER);        
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_snippet.setSettings(m_settings);
        FlowVariableRepository flowVarRepository =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        
        ValueReport<DataTableSpec> report = m_snippet.configure(null,
                flowVarRepository);

        if (report.hasWarnings()) {
            setWarningMessage(joinString(report.getWarnings(), "\n"));
        }
        if (report.hasErrors()) {
            throw new InvalidSettingsException(
                    joinString(report.getErrors(), "\n"));
        }

        for (FlowVariable flowVar : flowVarRepository.getModified()) {
            if (flowVar.getType().equals(Type.INTEGER)) {
                pushFlowVariableInt(flowVar.getName(), flowVar.getIntValue());
            } else if (flowVar.getType().equals(Type.DOUBLE)) {
                pushFlowVariableDouble(flowVar.getName(),
                        flowVar.getDoubleValue());
            } else {
                pushFlowVariableString(flowVar.getName(),
                        flowVar.getStringValue());
            }
        }
        return new DataTableSpec[] {report.getValue()};
    }

    /**
     * Concatenate strings with delimiter.
     * @param strings the string
     * @param delim the delimiter
     * @return concatenated string
     */
    private String joinString(final String[] strings, final String delim) {
    	if (null == strings || strings.length == 0) {
    		return "";
    	}
		StringBuilder b = new StringBuilder();
		b.append(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			b.append(delim);
			b.append(strings);
		}
		return b.toString();
	}


	/**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_snippet.setSettings(m_settings);
        FlowVariableRepository flowVarRepo =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        ValueReport<BufferedDataTable> out = m_snippet.execute(null, flowVarRepo, exec);
        if (out.hasWarnings()) {
        	setWarningMessage(joinString(out.getWarnings(), "\n"));
        }            
        if (out.hasErrors()) {
        	throw new RuntimeException(joinString(out.getErrors(), "\n"));
        }
    
        
        for (FlowVariable var : flowVarRepo.getModified()) {
            Type type = var.getType();
            if (type.equals(Type.INTEGER)) {
                pushFlowVariableInt(var.getName(), var.getIntValue());
            } else if (type.equals(Type.DOUBLE)) {
                pushFlowVariableDouble(var.getName(), var.getDoubleValue());
            } else { // case: type.equals(Type.STRING)
                pushFlowVariableString(var.getName(), var.getStringValue());
            }
        }
        return new BufferedDataTable[] {out.getValue()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RSnippetSettings s = new RSnippetSettings();
        s.loadSettings(settings);
        // TODO: Check settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals, nothing to reset.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }
}
