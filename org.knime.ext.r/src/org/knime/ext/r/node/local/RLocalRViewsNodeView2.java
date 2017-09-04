/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
 * The view of the <code>RLocalRViewsNodeModel</code> which is able to display an image created by a certain R command.
 * To display the image {@link org.knime.ext.r.node.RPlotterViewPanel} is used.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
public class RLocalRViewsNodeView2 extends NodeView<RLocalRViewsNodeModel2> {

    private final RPlotterViewPanel m_panel;

    /**
     * Creates a new instance of <code>RLocalRViewsNodeView</code> which displays a certain image.
     *
     * @param nodeModel the model associated with this view.
     */
    public RLocalRViewsNodeView2(final RLocalRViewsNodeModel2 nodeModel) {
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
        final RLocalRViewsNodeModel2 model = super.getNodeModel();
        final Image image = model.getResultImage();
        m_panel.update(image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // rien ne vas plus
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty
    }
}
