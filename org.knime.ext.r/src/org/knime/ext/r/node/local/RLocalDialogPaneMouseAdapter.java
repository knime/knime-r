/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   27.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JEditorPane;
import javax.swing.JList;

import org.knime.core.data.DataColumnSpec;

/**
 * A <code>MouseAdapter</code> which adds the selected column name to the
 * text pane (when the mouse is clicked) in a way that this column can be 
 * used by R.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalDialogPaneMouseAdapter extends MouseAdapter {
    
    private final JList m_list;
    
    private final JEditorPane m_pane;
    
    /**
     * Creates new instance of <code>RLocalDialogPaneMouseAdapter</code>
     * with a given list containing the column names and a given pane
     * to enter R commands. 
     * 
     * @param list the list containing the column names.
     * @param pane the pane to enter R commands.
     */
    public RLocalDialogPaneMouseAdapter(final JList list, 
            final JEditorPane pane) {
        m_list = list;
        m_pane = pane;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        Object o = m_list.getSelectedValue();
        if (o != null) {
            DataColumnSpec cspec = (DataColumnSpec) o;
            m_pane.replaceSelection("R$\"" + cspec.getName() + "\"");
            m_list.clearSelection();
            m_pane.requestFocus();
        }
    }
}
