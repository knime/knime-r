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
import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.util.FileUtil;

/**
 * Configurations for R To PMML node.
 *
 * @author Heiko Hofer
 */
final class RToPMMLNodeConfig extends RSnippetNodeConfig {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
	        "R Snippet");
	
	private final PortType m_inPortType;
	private File m_imageFile;

	public RToPMMLNodeConfig(final PortType inPortType) {
		m_inPortType = inPortType;
	}

	@Override
	protected Collection<PortType> getInPortTypes() {
		return Collections.singleton(m_inPortType);
	}
	
	@Override
	protected Collection<PortType> getOutPortTypes() {
		return Collections.singleton(PMMLPortObject.TYPE);
	}
	
	
	@Override
	protected String getScriptSuffix() {
		if (m_imageFile == null) {
			try {
				m_imageFile = FileUtil.createTempFile("R-to-pmml-", ".pmml");
			} catch (IOException e) {
				LOGGER.error("Cannot create temporary file.", e);
				throw new RuntimeException(e);
			}
			m_imageFile.deleteOnExit();
		}		
        // generate and write pmml
        return "write(knime.model.pmml, file=\"" + m_imageFile.getAbsolutePath().replace('\\', '/') + "\")\n\n";
	}

	public File getImageFile() {
		return m_imageFile;
	}
	
	@Override
	protected String getDefaultScript() {
	    return "library(pmml)\n"
	            + "knime.model.pmml <- toString(pmml(knime.model))\n";
	}

}
