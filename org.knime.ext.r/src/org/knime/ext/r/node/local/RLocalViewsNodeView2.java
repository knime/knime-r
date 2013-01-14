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
 * History
 *   18.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.Image;

import javax.swing.JScrollPane;

import org.knime.core.node.NodeView;
import org.knime.ext.r.node.RPlotterViewPanel;

/**
 * The view of the <code>RLocalViewsNodeModel</code> which is able to display
 * an image created by a certain R command. To display the image
 * {@link org.knime.ext.r.node.RPlotterViewPanel} is used.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeView2 extends NodeView<RLocalViewsNodeModel2> {

    private final RPlotterViewPanel m_panel;

    /**
     * Creates a new instance of <code>RLocalViewsNodeView</code> which displays
     * a certain image.
     *
     * @param nodeModel the model associated with this view.
     */
    public RLocalViewsNodeView2(final RLocalViewsNodeModel2 nodeModel) {
        super(nodeModel);
        m_panel = new RPlotterViewPanel();
        super.setComponent(new JScrollPane(m_panel));
    }

    /**
     * Updates the image to display.
     *
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        RLocalViewsNodeModel2 model = super.getNodeModel();
        Image image = model.getResultImage();
        m_panel.update(image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty
    }
}
