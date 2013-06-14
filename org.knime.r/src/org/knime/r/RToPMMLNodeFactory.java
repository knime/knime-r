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

import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortType;
import org.knime.ext.r.node.local.port.RPortObject;

/**
 * Factory for the <code>RSource</code> node.
 *
 * @author Heiko Hofer
 */
public class RToPMMLNodeFactory extends NodeFactory<RToPMMLNodeModel> {
	private RToPMMLNodeConfig m_config;

    /**
     * Empty default constructor.
     */
    public RToPMMLNodeFactory() {
    	this(RPortObject.TYPE);
    }

	public RToPMMLNodeFactory(final PortType inPortType) {
		m_config = new RToPMMLNodeConfig(inPortType);
	}
	

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }	

	/**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RSnippetNodeDialog(this.getClass(), m_config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RToPMMLNodeModel createNodeModel() {
    	return new RToPMMLNodeModel(m_config);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RToPMMLNodeModel> createNodeView(final int viewIndex,
            final RToPMMLNodeModel nodeModel) {
        if (viewIndex == 0) {
            return
                new ExtToolStdoutNodeView<RToPMMLNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return
                new ExtToolStderrNodeView<RToPMMLNodeModel>(nodeModel);
        }
        return null;
    }  
  
  
}
