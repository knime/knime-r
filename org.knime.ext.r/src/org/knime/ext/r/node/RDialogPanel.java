/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.r.node;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * Panel to enter R expressions.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RDialogPanel extends JPanel {

    private final JEditorPane m_textExpression;
    
    private final JList m_list;
    private final DefaultListModel m_listModel;
    
    /**
     * Creates a new dialog to enter R expressions with a default 
     * mouse listener.
     */
    public RDialogPanel() {
        this(null);
        m_list.addMouseListener(new DefaultMouseAdapter());
    }
    
    /**
     * Creates a new dialog to enter R expressions. 
     * @param mouseListener the listener to add to the column list.
     */
    public RDialogPanel(final MouseListener mouseListener) {
        super(new BorderLayout());
        super.setBorder(BorderFactory.createTitledBorder("R command"));
        
        // init editor pane
        m_textExpression = new JEditorPane();
        m_textExpression.setFont(new Font("Courier", Font.PLAIN, 12));
        super.add(new JScrollPane(m_textExpression), BorderLayout.CENTER);
        // init column list
        m_listModel = new DefaultListModel();
        m_list = new JList(m_listModel);
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setCellRenderer(new DataColumnSpecListCellRenderer());
        
        if (mouseListener != null) {
            m_list.addMouseListener(mouseListener);
        }
        
        JScrollPane scroll = new JScrollPane(m_list, 
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setMinimumSize(new Dimension(400, 300));
        super.add(scroll, BorderLayout.WEST);
    }
    
    /**
     * Updates the list of columns based on the given table spec by usage of
     * {@link RDialogPanel#update(DataTableSpec, boolean)} with rename set
     * true be default.
     * @param spec The spec to get columns from.
     */
    public final void update(final DataTableSpec spec) {
        update(spec, true);
    }
    
    /**
     * Updates the list of columns based on the given table spec.
     * @param spec The spec to get columns from.
     * @param rename if true columns are formatted in a way that they are 
     * compatible with the Rserv implementation.
     */
    public final void update(final DataTableSpec spec, final boolean rename) {
        m_listModel.removeAllElements();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec oldSpec = spec.getColumnSpec(i);
            DataType type = oldSpec.getType();
            
            DataColumnSpec cspec;
            if (rename) {
                String newName = RConnectionRemote.formatColumn(
                        oldSpec.getName());
                cspec = new DataColumnSpecCreator(newName, type).createSpec();
            } else {
                cspec = oldSpec;
            }
            
            if (type.isCompatible(IntValue.class)) {
                m_listModel.addElement(cspec);                
            } else
            if (type.isCompatible(DoubleValue.class)) {
                m_listModel.addElement(cspec);
            } else
            if (type.isCompatible(StringValue.class)) {
                m_listModel.addElement(cspec);
            }
        }
        repaint();
    }
    
    /**
     * @return complete text as string.
     */
    public String getText() {
        return m_textExpression.getText();
    }
    
    /**
     * @param str sets the given string as text. 
     */
    public void setText(final String str) {
        String text = str.trim();
        if (text.length() <= 0) {
            text = "R<-";
        }
        m_textExpression.setText(text);
        m_textExpression.setCaretPosition(text.length());
    }
    
    /**
     * @return expression text
     */
    public String[] getExpression() {
        String[] exps = m_textExpression.getText().split("\n");
        ArrayList<String> res = new ArrayList<String>();
        for (int i = 0; i < exps.length; i++) {
            exps[i] = exps[i].replace('\r', ' ');
            exps[i] = exps[i].replace('\t', ' ');
            exps[i] = exps[i].trim();
            if (exps[i].length() > 0) {
                res.add(exps[i]);
            }
        }
        return res.toArray(new String[0]);
    }

    /**
     * @param exp The expression to set
     */
    public void setExpression(final String[] exp) {
        m_textExpression.setText("");
        for (int i = exp.length; --i >= 0;) {
            m_textExpression.replaceSelection(exp[i] + "\n");
        }
        if (m_textExpression.getText().trim().length() == 0) {
            m_textExpression.setText("R<-");
        }
    }

    /**
     * @return the pane containing the R command to execute.
     */
    public final JEditorPane getEditorPane() {
        return m_textExpression;
    }
    
    /**
     * @return the list containing the column names.
     */
    public final JList getColumnList() {
        return m_list; 
    }
            
    
    /**
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class DefaultMouseAdapter extends MouseAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            Object o = m_list.getSelectedValue();
            if (o != null) {
                DataColumnSpec cspec = (DataColumnSpec) o;
                m_textExpression.replaceSelection(cspec.getName());
                m_list.clearSelection();
                m_textExpression.requestFocus();
            }
        }
    }
}
