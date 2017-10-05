/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.r.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.image.ImageContent;

/**
 * Displays the R result image.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("serial")
public final class RPlotterViewPanel extends JPanel {

    private final JLabel m_noPlotLabel = new JLabel("<No Plot>");

    private final ImagePanel m_imagePanel = new ImagePanel();

    /**
     * Creates a new panel with an empty label.
     *
     * @param image The content to show.
     */
    public RPlotterViewPanel(final ImageContent image) {
        this();
        update(image);
    }

    /**
     * Creates a new panel with an empty label.
     */
    public RPlotterViewPanel() {
        setLayout(new BorderLayout());
        add(m_noPlotLabel, BorderLayout.CENTER);
        add(m_imagePanel, BorderLayout.CENTER);

        m_imagePanel.setVisible(false);
    }

    /**
     * @param image The new image or null to display.
     */
    public void update(final ImageContent image) {
        if (image == null) {
            m_noPlotLabel.setVisible(true);
            m_imagePanel.setVisible(false);
        } else {
            m_noPlotLabel.setVisible(false);
            m_imagePanel.setImageContent(image);
            m_imagePanel.setVisible(true);
        }
        super.repaint();
    }

    private static final class ImagePanel extends JComponent {
        ImageContent m_content;

        /**
         * Update the image for this panel to show.
         *
         * @param content Image content to show in the panel
         */
        void setImageContent(final ImageContent content) {
            m_content = content;
            this.repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final Graphics gClip = g.create(getX(), getY(), getWidth(), getHeight());
            final Graphics2D g2d = (Graphics2D)gClip;

            m_content.paint(g2d, getWidth(), getHeight());
        }

        @Override
        public Dimension getPreferredSize() {
            return m_content.getPreferredSize();
        }
    }

}
