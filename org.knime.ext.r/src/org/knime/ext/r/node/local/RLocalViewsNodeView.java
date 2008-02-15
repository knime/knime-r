/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.Image;

import org.knime.core.node.NodeView;
import org.knime.ext.r.node.RPlotterViewPanel;

/**
 * The view of the <code>RLocalViewsNodeModel</code> which is able to display
 * an image created by a certain R command. To display the image 
 * {@link org.knime.ext.r.node.RPlotterViewPanel} is used.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeView extends NodeView<RLocalViewsNodeModel> {

    private final RPlotterViewPanel m_panel;
    
    /**
     * Creates a new instance of <code>RLocalViewsNodeView</code> which displays
     * a certain image.
     * 
     * @param nodeModel the model associated with this view.
     */
    public RLocalViewsNodeView(final RLocalViewsNodeModel nodeModel) {
        super(nodeModel);
        setViewTitle("Local R View: ");
        m_panel = new RPlotterViewPanel();
        super.setComponent(m_panel);
    }
    
    /**
     * Updates the image to display.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        RLocalViewsNodeModel model = (RLocalViewsNodeModel)super.getNodeModel();
        Image image = model.getResultImage();
        m_panel.update(image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}
