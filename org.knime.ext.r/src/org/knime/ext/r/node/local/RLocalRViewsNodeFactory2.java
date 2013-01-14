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
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * Factory of the <code>RLocalRViewsNodeModel</code>.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
public class RLocalRViewsNodeFactory2 extends RNodeFactory<RLocalRViewsNodeModel2> {

    /**
     * Empty default constructor.
     */
    public RLocalRViewsNodeFactory2() {
        super(RPreferenceInitializer.getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalViewsNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalRViewsNodeModel2 createNodeModel() {
        return new RLocalRViewsNodeModel2(getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RLocalRViewsNodeModel2> createNodeView(final int viewIndex,
            final RLocalRViewsNodeModel2 nodeModel) {
        if (viewIndex == 0) {
            return new RLocalRViewsNodeView2(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStdoutNodeView<RLocalRViewsNodeModel2>(nodeModel);
        } else if (viewIndex == 2) {
            return new ExtToolStderrNodeView<RLocalRViewsNodeModel2>(nodeModel);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
