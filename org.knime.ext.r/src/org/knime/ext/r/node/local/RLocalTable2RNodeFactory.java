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
 *   17.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolOutputNodeView;
import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * Factory for the <code>RLocalTable2RNodeFactory</code> node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
public class RLocalTable2RNodeFactory extends RNodeFactory<RLocalTable2RNodeModel> {

    /**
     * Empty default constructor.
     */
    public RLocalTable2RNodeFactory() {
        super(RPreferenceInitializer.getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalTable2RNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalTable2RNodeModel createNodeModel() {
        return new RLocalTable2RNodeModel(getRProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolOutputNodeView<RLocalTable2RNodeModel> createNodeView(
            final int viewIndex, final RLocalTable2RNodeModel nodeModel) {
        if (viewIndex == 0) {
            return
                new ExtToolStdoutNodeView<RLocalTable2RNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return
                new ExtToolStderrNodeView<RLocalTable2RNodeModel>(nodeModel);
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
