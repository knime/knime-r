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
 */
package org.knime.r.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

public class RObjectBrowser extends JTable {
    /** Generated serialVersionUID */
    private static final long serialVersionUID = 7537899041950123910L;

    RObjectBrowserModel m_model;

    public RObjectBrowser() {
        m_model = new RObjectBrowserModel();
        setModel(m_model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void updateData(final String[] objectNames, final String[] objectClasses) {
        m_model.updateData(objectNames, objectClasses);
    }

    private static class RObjectBrowserModel extends AbstractTableModel {
        /** Generated serialVersionUID */
        private static final long serialVersionUID = -4121185930197219249L;

        private String[] m_objectNames;

        private String[] m_objectClasses;

        public RObjectBrowserModel() {
            m_objectNames = new String[0];
            m_objectClasses = new String[0];
        }

        @Override
        public int getRowCount() {
            return m_objectNames.length;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(final int column) {
            if (column == 0) {
                return "Name";
            } else {
                return "Type";
            }
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                return m_objectNames[rowIndex];
            } else {
                return m_objectClasses[rowIndex];
            }
        }

        public void updateData(final String[] objectNames, final String[] objectClasses) {
            if ((objectNames != null) && (objectClasses != null)) {
                m_objectNames = objectNames;
                m_objectClasses = objectClasses;
            } else {
                m_objectNames = new String[0];
                m_objectClasses = new String[0];
            }
            fireTableDataChanged();
        }

    }

}
