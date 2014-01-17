/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node;

import java.awt.GridLayout;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Displays the R result image.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("serial")
public final class RPlotterViewPanel extends JPanel {

    private final JLabel m_label;

    /**
     * Creates a new panel with an empty label.
     * @param image The content to show.
     */
    public RPlotterViewPanel(final Image image) {
        m_label = new JLabel("<No Plot>");
        super.setLayout(new GridLayout(1, 1));
        super.add(m_label);
        update(image);
    }

    /**
     * Creates a new panel with an empty label.
     */
    public RPlotterViewPanel() {
        m_label = new JLabel("<No Plot>");
        super.add(m_label);
    }

    /**
     * @param image The new image or null to display.
     */
    public void update(final Image image) {
        if (image == null) {
            m_label.setIcon(null);
            m_label.setText("<No Plot>");
        } else {
            m_label.setText(null);
            m_label.setIcon(new ImageIcon(image));
        }
        super.repaint();
    }


}
