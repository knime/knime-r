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
package org.knime.r;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.io.pmml.read.PMMLImport;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;


/**
 * The <code>RToPMMLNodeModel</code> is the node model for the R To PMML node.
 *
 * @author Heiko Hofer
 */
public class RToPMMLNodeModel extends RSnippetNodeModel {
	private static final NodeLogger LOGGER = NodeLogger.getLogger("R To PMML");


	public RToPMMLNodeModel(final RToPMMLNodeConfig config) {
		super(config);
	}


	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new PortObjectSpec[]{null};
	}
	
	@Override
	protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
			throws Exception {
        super.execute(inData, exec);
        return postExecuteInternal();
	}
	
	private PortObject[] postExecuteInternal() throws Exception {
        if (getConfig().getImageFile().length() > 0) {
	        PMMLImport importer = new PMMLImport(getConfig().getImageFile(), true);
            return new PortObject[]{importer.getPortObject()};
        } else {
        	throw new RuntimeException("No PMML file was created by thr R-Script");
        }
	}

	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	// nothing to validate
    }
    
    /**
     * The saved image is loaded.
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals to load
    }

    /**
     * The created image is saved.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // to internals to reset  
    }
    
    private RToPMMLNodeConfig getConfig() {
    	return (RToPMMLNodeConfig)getRSnippetNodeConfig();
    }

}
