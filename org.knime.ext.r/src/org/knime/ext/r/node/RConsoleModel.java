/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2008
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

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;


/**
 * Executes R command and returns the result of the variable R as output.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RConsoleModel extends RNodeModel {

    private String[] m_expression = 
        new String[]{RDialogPanel.DEFAULT_R_COMMAND};
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RConsoleModel.class);
    
    /**
     * 
     * 
     */
    protected RConsoleModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        RConnection rconn = super.getRconnection();
        // send data to R server
        RConnectionRemote.sendData(rconn, inData[0], exec);
        // send expression to R server
        String[] expression = parseExpression(m_expression);
        for (String e : expression) {
            exec.setMessage("Executing R command: " + e);
            LOGGER.debug("voidEval: try(" + e + ")");
            rconn.voidEval("try(" + e + ")");
        }
        exec.setMessage("Recieving R result...");
        REXP rexp = rconn.eval("try(R)");
        LOGGER.debug("R: " + rexp.toString() + " " + rexp.getClass());
        
        if (rexp.isString()) {
            return new BufferedDataTable[]{readStrings(rexp, exec)};
        } else if (rexp.isNumeric()) {
            try {
                return new BufferedDataTable[]{readDoubleMatrix(rexp, exec)};
            } catch (REXPMismatchException rme) {
                if (rexp.isInteger()) {
                    return new BufferedDataTable[]{readIntegers(rexp, exec)};
                }
                return new BufferedDataTable[]{readDoubles(rexp, exec)};
            }
        } else if (rexp.isNull()) {
            return new BufferedDataTable[]{readList(rexp.asList(), exec)};
        } else if (rexp.isFactor()) {
            return new BufferedDataTable[]{readFactor(rexp.asFactor(), exec)};
        } else if (rexp.isList()) {
            return new BufferedDataTable[]{readList(rexp.asList(), exec)};
        } else if (rexp.isLogical()) {
            return new BufferedDataTable[]{readIntegers(rexp, exec)};
        } else if (rexp.isEnvironment()) {
            return new BufferedDataTable[]{readString(rexp, exec)};
        } else if (rexp.isLanguage()) {
            return new BufferedDataTable[]{readList(rexp.asList(), exec)};
        } else if (rexp.isExpression()) {
            return new BufferedDataTable[]{readList(rexp.asList(), exec)};
        } else if (rexp.isSymbol()) {
            return new BufferedDataTable[]{readStrings(rexp, exec)};
        } else if (rexp.isVector()) {
            return new BufferedDataTable[]{readString(rexp, exec)};
        } else if (rexp.isRaw()) {
            return new BufferedDataTable[]{readBytes(rexp, exec)};
        } else if (rexp.isComplex()) {
            return new BufferedDataTable[]{readString(rexp, exec)};
        } else {
            return new BufferedDataTable[]{readString(rexp, exec)};
        }
    }

    private DataColumnSpec createColumnSpec(final String columnName,
            final DataType columnType) {
        return new DataColumnSpecCreator(columnName, columnType).createSpec();
    }
    
    private BufferedDataTable readBytes(final REXP rexp, 
            final ExecutionContext exec)
            throws CanceledExecutionException, REXPMismatchException {
        byte[] bytes = rexp.asBytes();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R" + (i + 1), StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < bytes.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / bytes.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    String.valueOf(bytes[i])));
        }
        dc.close();
        return dc.getTable();
        
    }
    
    private BufferedDataTable readList(
            final RList list, final ExecutionContext exec) 
            throws CanceledExecutionException, REXPMismatchException {
        DataColumnSpec[] cspec = new DataColumnSpec[list.size()];
        Object[] object = new Object[cspec.length];
        int nrRows = 0;
        for (int i = 0; i < cspec.length; i++) {
            DataType type;
            if (list.at(i).isFactor()) {
                object[i] = list.at(i).asStrings();
                type = StringCell.TYPE;
            } else if (list.at(i).isInteger()) {
                object[i] = list.at(i).asIntegers();
                type = IntCell.TYPE;
            } else if (list.at(i).isNumeric()) {
                object[i] = list.at(i).asDoubles();
                type = DoubleCell.TYPE;
            } else {
                object[i] = list.at(i).asStrings();
                type = StringCell.TYPE;
            }
            
            nrRows = list.at(i).asStrings().length;
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator(list.names.get(i).toString(), type);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < nrRows; i++) {
            exec.checkCanceled();
            DataCell[] row = new DataCell[object.length];
            for (int j = 0; j < row.length; j++) {
                if (cspec[j].getType() == IntCell.TYPE) {
                    row[j] = new IntCell(((int[]) object[j])[i]);
                } else if (cspec[j].getType() == DoubleCell.TYPE) {
                    double dblValue = ((double[]) object[j])[i];
                    if (Double.isNaN(dblValue)) {
                        row[j] = DataType.getMissingCell();
                    } else {
                        row[j] = new DoubleCell(dblValue);
                    }
                } else {
                    String strValue = ((String[]) object[j])[i];
                    if (strValue == null) {
                        row[j] = DataType.getMissingCell();
                    } else {
                        row[j] = new StringCell(strValue);
                    }
                }   
            }
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    row));
        }
        dc.close();
        return dc.getTable();
    }
    
    private BufferedDataTable readFactor(
            final RFactor fac, final ExecutionContext exec) 
            throws CanceledExecutionException {
        String[] strings = fac.asStrings();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R" + (i + 1), StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < fac.size(); i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / strings.length);
            StringCell rowKey = new StringCell("Row" + (i + 1));
            String strValue = strings[fac.indexAt(i)];
            if (strValue == null) {
                dc.addRowToTable(
                        new DefaultRow(rowKey, DataType.getMissingCell()));
            } else {
                dc.addRowToTable(new DefaultRow(rowKey, strValue));
            }
        }
        dc.close();
        return dc.getTable();
    }
    
    private BufferedDataTable readString(
            final REXP rexp, final ExecutionContext exec) 
            throws REXPMismatchException {
        String string = rexp.asString();
        DataColumnSpec cspec = createColumnSpec("R1", StringCell.TYPE);
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        DataRow row;
        if (string == null) {
            row = new DefaultRow(new StringCell("Row1"), 
                DataType.getMissingCell());
        } else {
            row = new DefaultRow(new StringCell("Row1"), string);
        }
        dc.addRowToTable(row);
        dc.close();
        return dc.getTable();
    }
   
    private BufferedDataTable readDoubles(
            final REXP rexp, final ExecutionContext exec) 
            throws REXPMismatchException, CanceledExecutionException {
        double[] matrix = rexp.asDoubles();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R" + (i + 1), DoubleCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    matrix[i]));
        }
        dc.close();
        return dc.getTable();
    }
    
    private BufferedDataTable readStrings(
            final REXP rexp, final ExecutionContext exec) 
            throws REXPMismatchException, CanceledExecutionException {
        String[] matrix = rexp.asStrings();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R" + (i + 1), StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            StringCell rowKey = new StringCell("Row" + (i + 1));
            if (matrix[i] == null) {
                dc.addRowToTable(new DefaultRow(rowKey, 
                        DataType.getMissingCell()));
            } else {
                dc.addRowToTable(new DefaultRow(rowKey, matrix[i]));
            }
        }
        dc.close();
        return dc.getTable();
    }
    
    private BufferedDataTable readDoubleMatrix(
            final REXP rexp, final ExecutionContext exec) 
            throws REXPMismatchException, CanceledExecutionException {
        double[][] matrix = rexp.asDoubleMatrix();
        DataColumnSpec[] cspec;
        if (matrix.length == 0) {
            cspec = new DataColumnSpec[0];
        } else {
            cspec = new DataColumnSpec[matrix[0].length];
            for (int i = 0; i < cspec.length; i++) {
                DataColumnSpecCreator colspeccreator = 
                    new DataColumnSpecCreator("R" + (i + 1), DoubleCell.TYPE);
                cspec[i] = colspeccreator.createSpec();
            }
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    matrix[i]));
        }
        dc.close();
        return dc.getTable();
    }
    
    private BufferedDataTable readIntegers(
            final REXP rexp, final ExecutionContext exec) 
            throws REXPMismatchException, CanceledExecutionException {
        int[] matrix = rexp.asIntegers();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R" + (i + 1), IntCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        BufferedDataContainer dc = 
            exec.createDataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    new int[]{matrix[i]}));
        }
        dc.close();
        return dc.getTable();
    }



    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // remove R variable
        try {
            RConnection rconn = getRconnection();
            rconn.voidEval("try(rm(R))");
        } catch (Throwable t) {
            LOGGER.debug("Could not remove variable R: ", t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[1];
    }

    /**
     * Tests the given expressions if they contain at least one statement
     * where data is assigned to variable R. 
     * 
     * @param rexps a String array containing the expression to check.
     * @throws InvalidSettingsException If the given statements do not contain
     * at least one statement where data is assigned to variable R.
     */
    public static final void testExpressions(final String[] rexps)
            throws InvalidSettingsException {
        for (int i = 0; i < rexps.length; i++) {
            String test = rexps[i].replace(" ", "");
            if (test.contains("R<-")) {
                // ok, we have an result in R
                return;
            }
        }
        throw new InvalidSettingsException("The result has to be provided"
                + " inside the variable R");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        RDialogPanel.setExpressionsTo(settings, m_expression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_expression = RDialogPanel.getExpressionsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        testExpressions(RDialogPanel.getExpressionsFrom(settings));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }
    
}
