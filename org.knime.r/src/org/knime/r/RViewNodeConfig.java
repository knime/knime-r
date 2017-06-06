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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.util.FileUtil;

import com.sun.jna.Platform;

/**
 * Configurations for R nodes with image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeConfig extends RSnippetNodeConfig {
	private static final NodeLogger LOGGER = NodeLogger.getLogger("R Snippet");

	private final PortType m_inPortType;
	private File m_imageFile;

	private RViewNodeSettings m_settings;

	public RViewNodeConfig(final PortType inPortType) {
		m_inPortType = inPortType;
	}

	@Override
    public Collection<PortType> getInPortTypes() {
		return Collections.singleton(m_inPortType);
	}

	@Override
	protected Collection<PortType> getOutPortTypes() {
		return Collections.singleton(ImagePortObject.TYPE);
	}

	@Override
	protected String getScriptPrefix() {
		final File imageFile = getImageFile();
		final StringBuilder prefix = new StringBuilder();

		String bitmapType = "";
		if (Platform.isMac()) {
			bitmapType = ",bitmapType='cairo'";
			prefix.append("library('Cairo');");
		}

		prefix.append("options(device = 'png'" + bitmapType + ")").append("\n");
		prefix.append("png(\"" + imageFile.getAbsolutePath().replace('\\', '/') + "\"" + ", width="
				+ m_settings.getImageWidth() + ", height=" + m_settings.getImageHeight() + ", pointsize="
				+ m_settings.getTextPointSize() + ", bg=\"" + m_settings.getImageBackgroundColor() + "\"" + ", res="
				+ m_settings.getImageResolution() + ")");
		return prefix.toString();
	}

	@Override
	protected String getScriptSuffix() {
		return "dev.off()";
	}

	/**
	 * Non-null image file to use for this current node. Lazy-initialized to
	 * temp location.
	 */
	public File getImageFile() {
		if (m_imageFile == null) {
			try {
				m_imageFile = FileUtil.createTempFile("R-view-", ".png");
			} catch (final IOException e) {
				LOGGER.error("Cannot create temporary file.", e);
				throw new RuntimeException(e);
			}
			m_imageFile.deleteOnExit();
		}
		return m_imageFile;
	}

	public void setSettings(final RViewNodeSettings settings) {
		m_settings = settings;
	}

	@Override
	public String getDefaultScript() {
		if (BufferedDataTable.TYPE.equals(m_inPortType)) {
			return "plot(knime.in)\n";
		} else {
			return "plot(iris)\n";
		}
	}
}
