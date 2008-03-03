/*
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.r.node;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;

/**
 * <code>NodeFactory</code> for R Console.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RConsoleFactory extends GenericNodeFactory<RConsoleModel> {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RConsoleModel createNodeModel() {
        return new RConsoleModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<RConsoleModel> createNodeView(final int viewIndex,
            final RConsoleModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RConsoleDialogPane();
    }
}
