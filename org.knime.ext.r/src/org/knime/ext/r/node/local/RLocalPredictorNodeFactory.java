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
 */
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolOutputNodeView;
import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalPredictorNodeFactory 
        extends RNodeFactory<RLocalPredictorNodeModel> {
    
    /**
     * Create a new factory class for a local R predictor node.
     */
    public RLocalPredictorNodeFactory() {
        super(RPreferenceInitializer.getRProvider());
    }

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
        return new RLocalPredictorNodeModel(getRProvider());
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
