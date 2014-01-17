/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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
 */
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolOutputNodeView;
import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
public class RLocalR2TableNodeFactory extends RNodeFactory<RLocalR2TableNodeModel> {

    /**
     * Create a new factory class for a R to Table (Local) node.
     */
    public RLocalR2TableNodeFactory() {
        super(RPreferenceInitializer.getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalR2TableNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalR2TableNodeModel createNodeModel() {
        return new RLocalR2TableNodeModel(getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolOutputNodeView<RLocalR2TableNodeModel> createNodeView(
            final int viewIndex, final RLocalR2TableNodeModel nodeModel) {
        if (viewIndex == 0) {
            return
                new ExtToolStdoutNodeView<RLocalR2TableNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return
                new ExtToolStderrNodeView<RLocalR2TableNodeModel>(nodeModel);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
