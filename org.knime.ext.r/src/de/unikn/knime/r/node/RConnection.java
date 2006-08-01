/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
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
package de.unikn.knime.r.node;

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
import org.rosuda.JRclient.REXP;
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
    
    static final String formatColumn(final String name) {
        String newName = name.replaceAll("[^a-zA-Z0-9]", "_");
        if (!newName.equals(name)) {
            LOGGER.warn("Column name changed: " + name + " -> " + newName);
        }
        return newName;
    }
   
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

        // transfere data chunkwise
        Object[] data = new Object[types.length];
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
                    case 1 : {
                        int[] value;
                        if (data[i] == null) {
                            value = new int[max];
                            data[i] = value;
                        } else {
                            value = (int[]) data[i];
                        }
                        value[z] = ((IntValue) cell).getIntValue();
                        break;
                    }
                    case 2 : {
                        double[] value;
                        if (data[i] == null) {
                            value = new double[max];
                            data[i] = value;
                        } else {
                            value = (double[]) data[i];
                        }
                        value[z] = ((DoubleValue) cell).getDoubleValue();
                        break;
                    }
                    case 3 : { 
                        String[] value; 
                        if (data[i] == null) {
                            value = new String[max];
                            data[i] = value;
                        } else {
                            value = (String[]) data[i];
                        }
                        value[z] = ((StringValue) cell).getStringValue();
                        break;
                    }
                }
            }
            z++;
            if (z % max == 0) {
                for (int i = 0; i < data.length; i++) {
                    String colName = formatColumn(
                            spec.getColumnSpec(i).getName());
                    switch (types[i]) {
                        case 1 : {
                            conn.assign("ColTmp" + i, (int[]) data[i]);
                            break;
                        }
                        case 2 : {
                            conn.assign("ColTmp" + i, (double[]) data[i]);
                            break;
                        }
                        case 3 : {
                            conn.assign("ColTmp" + i, new REXP((String[]) data[i]));
                            break;
                        }
                    }
                    conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" + i + ")");
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
                switch (types[i]) {
                    case 1 : {
                        int[] copy = new int[z];
                        System.arraycopy(data[i], 0, copy, 0, z);
                        conn.assign("ColTmp" + i, copy); 
                        break;
                    }
                    case 2 : {
                        double[] copy = new double[z];
                        System.arraycopy(data[i], 0, copy, 0, z);
                        conn.assign("ColTmp" + i, copy); 
                        break;
                    }
                    case 3 : {
                        String[] copy = new String[z];
                        System.arraycopy(data[i], 0, copy, 0, z);
                        conn.assign("ColTmp" + i, new REXP(copy)); 
                        break;
                    }
                }
                conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" + i + ")");
                data[i] = null;
            }
        }
    }
    
}
