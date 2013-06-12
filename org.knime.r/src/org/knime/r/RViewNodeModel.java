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
 */
package org.knime.r;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.util.FileUtil;


/**
 * The <code>RViewNodeModel</code> is the node model for the r-nodes with an image output.
 *
 * @author Heiko Hofer
 */
public class RViewNodeModel extends RSnippetNodeModel {
	private static final NodeLogger LOGGER = NodeLogger.getLogger("R View");
	
	private RViewNodeSettings m_settings;
	
	private Image m_resultImage;

    
    /** Output spec for a PNG image. */
    private static final ImagePortObjectSpec OUT_SPEC =
        new ImagePortObjectSpec(PNGImageContent.TYPE);
    
    private static final String INTERNAL_FILE_NAME = "Rplot";

	public RViewNodeModel(final RViewNodeConfig config) {
		super(config);
		m_settings = new RViewNodeSettings(getSettings());
		getConfig().setSettings(m_settings);
	}


	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new PortObjectSpec[] {OUT_SPEC};
	}
	
	@Override
	protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
			throws Exception {
        super.execute(inData, exec);
        return postExecuteInternal();

	}
	
	private PortObject[] postExecuteInternal() throws Exception {
        if (getConfig().getImageFile().length() > 0) {
	        // create image after execution.
	        FileInputStream fis = new FileInputStream(getConfig().getImageFile());
	        PNGImageContent content = new PNGImageContent(fis);
	        fis.close();
	        m_resultImage = content.getImage();
	        return new PortObject[] {new ImagePortObject(content, OUT_SPEC)};
        } else {
        	throw new RuntimeException("No Image was created by thr R-Script");
        }
	}


	@Override
	public PortObject[] reExecute(final RSnippetViewContent content,
			final PortObject[] data, final ExecutionContext exec)
			throws CanceledExecutionException {
		super.reExecute(content, data, exec);
		try {
			return postExecuteInternal();
		} catch (Exception e) {
			if (e instanceof CanceledExecutionException) {
				throw (CanceledExecutionException)e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}
	
	
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	// TODO: validate settings
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
        super.loadInternals(nodeInternDir, exec);

        File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
        if (file.exists() && file.canRead()) {
            File pngFile = File.createTempFile(INTERNAL_FILE_NAME, ".png", new File(KNIMEConstants.getKNIMETempDir()));
            FileUtil.copy(file, pngFile);
            InputStream is = new FileInputStream(pngFile);
            m_resultImage = new PNGImageContent(is).getImage();
            is.close();
        }
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
        super.saveInternals(nodeInternDir, exec);

        if (m_resultImage != null) {
            File file = new File(nodeInternDir, INTERNAL_FILE_NAME + ".png");
            FileUtil.copy(getConfig().getImageFile(), file);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultImage = null;
        // clear image file contents
	    if (getConfig().getImageFile() != null) {
			try {
				FileOutputStream erasor = new FileOutputStream(getConfig().getImageFile());
				erasor.write((new String()).getBytes());
			    erasor.close();    
			} catch (FileNotFoundException e) {
				LOGGER.error("Temporary file is removed.", e);
			} catch (IOException e) {
				LOGGER.error("Cannot write temporary file.", e);
			}
	    }  
    }
    
    private RViewNodeConfig getConfig() {
    	return (RViewNodeConfig)getRSnippetNodeConfig();
    }


	public Image getResultImage() {
		return m_resultImage;
	}

}
