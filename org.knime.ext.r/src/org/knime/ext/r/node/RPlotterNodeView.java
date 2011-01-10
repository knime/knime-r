/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node;

import java.awt.Image;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the R plotter.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@Deprecated
public class RPlotterNodeView extends NodeView<RPlotterNodeModel> {
    private final RPlotterViewPanel m_panel;

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: <code>RPlotterNodeModel</code>)
     */
    protected RPlotterNodeView(final RPlotterNodeModel nodeModel) {
        super(nodeModel);
        m_panel = new RPlotterViewPanel();
        super.setComponent(m_panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        RPlotterNodeModel model = super.getNodeModel();
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
