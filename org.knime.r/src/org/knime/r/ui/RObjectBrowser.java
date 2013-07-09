package org.knime.r.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

public class RObjectBrowser extends JTable {
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
			if (objectNames != null && objectClasses != null) {
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
