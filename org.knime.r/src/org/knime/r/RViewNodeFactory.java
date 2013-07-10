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
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortType;

/**
 * Factory for the R View node.
 *
 * @author Heiko Hofer
 */
public class RViewNodeFactory extends NodeFactory<RViewNodeModel> {
	private RViewNodeConfig m_portType;

    /**
     * Empty default constructor.
     */
    public RViewNodeFactory() {
    	this(BufferedDataTable.TYPE);
    }

	public RViewNodeFactory(final PortType inPortType) {
		m_portType = new RViewNodeConfig(inPortType);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RViewNodeDialog(this.getClass(), m_portType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RViewNodeModel createNodeModel() {
    	return new RViewNodeModel(m_portType);
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
    public NodeView<RViewNodeModel> createNodeView(final int viewIndex,
            final RViewNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new RViewNodeView(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStdoutNodeView<RViewNodeModel>(nodeModel);
        } else if (viewIndex == 2) {
            return new ExtToolStderrNodeView<RViewNodeModel>(nodeModel);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
