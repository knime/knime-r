/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
 *   15.09.2008 (thiel): created
 */
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolOutputNodeView;
import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalPredictorNodeFactory 
extends NodeFactory<RLocalPredictorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalPredictorNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalPredictorNodeModel createNodeModel() {
        return new RLocalPredictorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolOutputNodeView<RLocalPredictorNodeModel> createNodeView(
            final int viewIndex, final RLocalPredictorNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new ExtToolStderrNodeView<RLocalPredictorNodeModel>(
                    nodeModel);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
