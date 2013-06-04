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

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.interactive.InteractiveNodeFactoryExtension;

/**
 * Factory for the <code>RSnippetNodeFactory</code> node.
 *
 * @author Heiko Hofer
 */
public class RSnippetNodeFactory extends NodeFactory<RSnippetNodeModel> 
		implements InteractiveNodeFactoryExtension<RSnippetNodeModel, RSnippetViewContent> {
//	private final RPreferenceProvider m_pref; 
	
    /**
     * Empty default constructor.
     */
    public RSnippetNodeFactory() {
//        m_pref = RPreferenceInitializer.getRProvider();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RSnippetNodeDialog(this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RSnippetNodeModel createNodeModel() {
//        return new RSnippetNodeModel(m_pref);
    	return new RSnippetNodeModel();
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
    public NodeView<RSnippetNodeModel> createNodeView(final int viewIndex,
            final RSnippetNodeModel nodeModel) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

	@Override
	@SuppressWarnings("unchecked")
	public RSnippetNodeView createInteractiveView(final RSnippetNodeModel model) {
		return new RSnippetNodeView(model, this.getClass());
	}
}
