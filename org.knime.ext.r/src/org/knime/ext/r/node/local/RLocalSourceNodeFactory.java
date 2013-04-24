/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * @since 2.8
 */
public class RLocalSourceNodeFactory extends RNodeFactory<RLocalSourceNodeModel> {

    /**
     * Create a new factory class for a R to Table (Local) node.
     */
    public RLocalSourceNodeFactory() {
        super(RPreferenceInitializer.getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalSourceNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalSourceNodeModel createNodeModel() {
        return new RLocalSourceNodeModel(getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolOutputNodeView<RLocalSourceNodeModel> createNodeView(
            final int viewIndex, final RLocalSourceNodeModel nodeModel) {
        if (viewIndex == 0) {
            return
                new ExtToolStdoutNodeView<RLocalSourceNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return
                new ExtToolStderrNodeView<RLocalSourceNodeModel>(nodeModel);
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
