/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.rosuda.JRclient.RSrvException;
import org.rosuda.JRclient.Rconnection;


/**
 * Utility class for sending data to a R server.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class RConnection {
        
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RConnection.class);

    private RConnection() {
        
    }
    
    /**
     * Replaces illegal characters in the specified string. Legal characters are
     * a-z, A-Z and 0-9. All others will be replaced by an underscore.
     * 
     * @param name the string to check and to replace illegal characters in.
     * @return a string containing only a-z, A-Z, 0-9 and _. All other
     *         characters got replaced by an underscore ('_').
     */
    static final String formatColumn(final String name) {
        String newName = name.replaceAll("[^a-zA-Z0-9]", "_");
        if (!newName.equals(name)) {
            LOGGER.warn("Column name changed: " + name + " -> " + newName);
        }
        return newName;
    }
   
    /**
     * Sends the entire table to the R server for types which are compatible
     * to IntValue.class, DoubleValue.class, and StringValue.class.
     * @param conn The connection to the R server.
     * @param inData The data to send.
     * @param exec Used to report progress.
     * @throws RSrvException If the server throws an execption.
     * @throws CanceledExecutionException If canceled.
     */
    static final void sendData(
            final Rconnection conn, final BufferedDataTable inData,
            final ExecutionMonitor exec) 
            throws RSrvException, CanceledExecutionException {
        // prepare data
        DataTableSpec spec = inData.getDataTableSpec();
        int[] types = new int[spec.getNumColumns()];
        for (int i = 0; i < types.length; i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            DataType type = cspec.getType();
            if (type.isCompatible(IntValue.class)) {
                types[i] = 1;
            } else
            if (type.isCompatible(DoubleValue.class)) {
                types[i] = 2;
            } else
            if (type.isCompatible(StringValue.class)) {
                types[i] = 3;
            } else {
                types[i] = -1; // unsupported type
            }
            // init column empty
            String cmd = formatColumn(cspec.getName()) + " <- c()";
            LOGGER.info(cmd);
            conn.eval(cmd);
        }

        // transfere data chunkwise; one builder per column
        StringBuilder[] data = new StringBuilder[types.length];
        int z = 0; // buffer size
        int max = 1000; // send data after max number of rows
        int nsend = 0; // number of max packets sent
        int rowCount = 0;
        for (DataRow row : inData) { // rows
            exec.checkCanceled();
            exec.setProgress(1.0 * rowCount++ / inData.getRowCount());
            for (int i = 0; i < data.length; i++) { // columns
                DataCell cell = row.getCell(i);
                switch (types[i]) {
                    case 1 : // int
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append(((IntValue) cell).getIntValue());
                        }
                        break;
                    case 2 : // double
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append(
                                    ((DoubleValue) cell).getDoubleValue());
                        }
                        break;
                    case 3 : // String
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append("\""
                                    + ((StringValue) cell).getStringValue() 
                                    + "\"");
                        }
                        break;
                }
            }
            z++;
            if (z % max == 0) {
                for (int i = 0; i < data.length; i++) {
                    String colName = formatColumn(
                            spec.getColumnSpec(i).getName());
                    conn.eval("ColTmp" + i + " <- c(" + data[i].toString() 
                            + ")");
                    conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" 
                            + i + ")");
                    data[i] = null;
                }
                z = 0;
                nsend++;
            }
        }
        
        if (z > 0 && z < max) {
            for (int i = 0; i < data.length; i++) {
                String colName = formatColumn(
                        spec.getColumnSpec(i).getName());
                conn.eval("ColTmp" + i + " <- c(" + data[i].toString() + ")");
                conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" + i 
                        + ")");
                data[i] = null;
            }
        }
    }
    
}
