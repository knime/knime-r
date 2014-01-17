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

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.util.FileUtil;

/**
 * Configurations for R nodes with image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeConfig extends RSnippetNodeConfig {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
	        "R Snippet");
	
	private final PortType m_inPortType;
	private File m_imageFile;

	private RViewNodeSettings m_settings;

	public RViewNodeConfig(final PortType inPortType) {
		m_inPortType = inPortType;
	}

	@Override
	protected Collection<PortType> getInPortTypes() {
		return Collections.singleton(m_inPortType);
	}
	
	@Override
	protected Collection<PortType> getOutPortTypes() {
		return Collections.singleton(ImagePortObject.TYPE);
	}
	
	@Override
	protected String getScriptPrefix() {
		if (m_imageFile == null) {
			try {
				m_imageFile = FileUtil.createTempFile("R-view-", ".png");
			} catch (IOException e) {
				LOGGER.error("Cannot create temporary file.", e);
				throw new RuntimeException(e);
			}
			m_imageFile.deleteOnExit();
		}

        return "png(\"" + m_imageFile.getAbsolutePath().replace('\\', '/') + "\""
        + ", width=" + m_settings.getImageWidth()
        + ", height=" + m_settings.getImageHeight()
        + ", pointsize=" + m_settings.getTextPointSize()
        + ", bg=\"" + m_settings.getImageBackgroundColor() + "\""
        + ", res=" + m_settings.getImageResolution() + ");\n";
	}
	
	@Override
	protected String getScriptSuffix() {
		return "\ndev.off();";
	}

	public File getImageFile() {
		return m_imageFile;
	}

	public void setSettings(final RViewNodeSettings settings) {
		m_settings = settings;
	}
	
	@Override
	String getDefaultScript() {
	    if (BufferedDataTable.TYPE.equals(m_inPortType)) {
	        return "plot(knime.in)\n";
	    } else {
	        return "plot(iris)\n";
	    }
	}
}
