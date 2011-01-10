/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.ext.r.node.local;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class R2PMMLNodeFactory extends RNodeFactory<R2PMMLNodeModel> {
    
    /**
     * Creates a new factor for the R2PMML node.
     */
    public R2PMMLNodeFactory() {
        super(RPreferenceInitializer.getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new R2PMMLNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R2PMMLNodeModel createNodeModel() {
        return new R2PMMLNodeModel(getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<R2PMMLNodeModel> createNodeView(final int viewIndex, 
            final R2PMMLNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
