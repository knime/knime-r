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
package org.knime.ext.r.node;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * Utility class for sending data to a R server.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RConnectionRemote {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RConnectionRemote.class);

    private RConnectionRemote() {
        // empty
    }

    /**
     * Replaces illegal characters in the specified string. Legal characters are a-z, A-Z and 0-9. All others will be
     * replaced by a dot.
     *
     * @param name the string to check and to replace illegal characters in.
     * @return a string containing only a-z, A-Z and 0-9. All other characters got replaced by a dot.
     */
    private static final String formatColumn(final String name) {
        return name.replaceAll("[^a-zA-Z0-9]", ".");
    }

    /**
     * Renames all column names by replacing all characters which are not numeric or letters.
     * 
     * @param spec spec to replace column names
     * @return new spec with replaced column names
     */
    public static final DataTableSpec createRenamedDataTableSpec(final DataTableSpec spec) {
        final DataColumnSpec[] cspecs = new DataColumnSpec[spec.getNumColumns()];
        final Set<String> newColNames = new HashSet<String>();
        for (int i = 0; i < cspecs.length; i++) {
            final DataColumnSpecCreator cr = new DataColumnSpecCreator(spec.getColumnSpec(i));
            final String oldName = spec.getColumnSpec(i).getName();
            // uniquify formatted column name
            final String newName = RConnectionRemote.formatColumn(oldName);
            int colIdx = 0;
            String uniqueColName = newName;
            if (!oldName.equals(newName)) {
                while (newColNames.contains(uniqueColName) || spec.containsName(uniqueColName)) {
                    uniqueColName = newName + "_" + colIdx++;
                }
                cr.setName(uniqueColName);
                newColNames.add(uniqueColName);
                LOGGER.info("Original column \"" + oldName + "\" was renamed to \"" + uniqueColName + "\".");
            }
            cspecs[i] = cr.createSpec();
        }
        return new DataTableSpec(spec.getName(), cspecs);
    }

    /**
     * Sends the entire table to the R server for types which are compatible to IntValue.class, DoubleValue.class, and
     * StringValue.class.
     * 
     * @param conn The connection to the R server.
     * @param inData The data to send.
     * @param exec Used to report progress.
     * @throws RserveException If the server throws an exception.
     * @throws CanceledExecutionException If canceled.
     */
    static final void sendData(final RConnection conn, final BufferedDataTable inData, final ExecutionMonitor exec)
        throws RserveException, CanceledExecutionException {
        exec.setMessage("Start sending data to R server...");
        // prepare data
        final DataTableSpec spec = RConnectionRemote.createRenamedDataTableSpec(inData.getDataTableSpec());
        final int[] types = new int[spec.getNumColumns()];
        for (int i = 0; i < types.length; i++) {
            final DataColumnSpec cspec = spec.getColumnSpec(i);
            final DataType type = cspec.getType();
            if (type.isCompatible(IntValue.class)) {
                types[i] = 1;
            } else if (type.isCompatible(DoubleValue.class)) {
                types[i] = 2;
            } else if (type.isCompatible(StringValue.class)) {
                types[i] = 3;
            } else {
                types[i] = -1; // unsupported type
            }
            // init column empty
            final String cmd = cspec.getName() + " <- c()";
            LOGGER.info(cmd);
            conn.eval(cmd);
        }

        // transfer data chunk-wise; one builder per column
        final StringBuilder[] data = new StringBuilder[types.length];
        int z = 0; // buffer size
        final int max = 1000; // send data after max number of rows
        int nsend = 0; // number of max packets sent
        int rowCount = 0;
        for (final DataRow row : inData) { // rows
            exec.checkCanceled();
            exec.setProgress((1.0 * rowCount++) / inData.getRowCount());
            for (int i = 0; i < data.length; i++) { // columns
                final DataCell cell = row.getCell(i);
                switch (types[i]) {
                    case 1: // int
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append(((IntValue)cell).getIntValue());
                        }
                        break;
                    case 2: // double
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append(((DoubleValue)cell).getDoubleValue());
                        }
                        break;
                    case 3: // String
                        if (data[i] == null) {
                            data[i] = new StringBuilder();
                        } else {
                            data[i].append(',');
                        }
                        if (cell.isMissing()) {
                            data[i].append("NA");
                        } else {
                            data[i].append("\"" + ((StringValue)cell).getStringValue() + "\"");
                        }
                        break;
                }
            }
            z++;
            if ((z % max) == 0) {
                final String msg = "Sending data chunk " + (nsend + 1) + " (with " + max + " rows).";
                LOGGER.info(msg);
                exec.setMessage(msg);
                for (int i = 0; i < data.length; i++) {
                    final String colName = spec.getColumnSpec(i).getName();
                    conn.eval("ColTmp" + i + " <- c(" + data[i].toString() + ")");
                    conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" + i + ")");
                    data[i] = null;
                }
                z = 0;
                nsend++;
            }
        }

        if ((z > 0) && (z < max)) {
            for (int i = 0; i < data.length; i++) {
                final String colName = spec.getColumnSpec(i).getName();
                conn.eval("ColTmp" + i + " <- c(" + data[i].toString() + ")");
                conn.eval(colName + " <- " + "c(" + colName + ",ColTmp" + i + ")");
                data[i] = null;
            }
        }

        final StringBuilder colList = new StringBuilder();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                colList.append(",");
            }
            final String colName = spec.getColumnSpec(i).getName();
            colList.append(colName);
        }
        conn.eval("R <- data.frame(" + colList.toString() + ")");

    }

}
