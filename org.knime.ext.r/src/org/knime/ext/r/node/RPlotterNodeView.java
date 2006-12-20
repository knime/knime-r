/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2007
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

import java.awt.Image;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the R plotter.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPlotterNodeView extends NodeView {
    private final RPlotterViewPanel m_panel;

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: <code>RPlotterNodeModel</code>)
     */
    protected RPlotterNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        m_panel = new RPlotterViewPanel();
        super.setComponent(m_panel);
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        RPlotterNodeModel model = (RPlotterNodeModel) super.getNodeModel();
        Image image = model.getResultImage();        
        m_panel.update(image);
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {

    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {

    }
}
