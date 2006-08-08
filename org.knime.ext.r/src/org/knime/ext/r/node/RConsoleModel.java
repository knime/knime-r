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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.rosuda.JRclient.RBool;
import org.rosuda.JRclient.REXP;
import org.rosuda.JRclient.Rconnection;


/**
 * Executes R command and returns the result of the variable R as output.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RConsoleModel extends RNodeModel {

    private String[] m_expression = new String[0];
    
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
     * @see org.knime.core.node.NodeModel
     *      #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        Rconnection rconn = getRconnection();
        // send data to R Server
        RConnection.sendData(rconn, inData[0], exec);
        // send expression to R Server
        for (int i = 0; i < m_expression.length; i++) {
            LOGGER.debug("eval: " + m_expression[i]);
            rconn.voidEval("try(" + m_expression[i] + ")");
            LOGGER.debug("sucessful");
        }
        REXP rexp = rconn.eval("try(R)");
        LOGGER.debug("R: " + rexp.toString());
        switch (rexp.getType()) {
            case REXP.XT_ARRAY_BOOL : {
                BufferedDataTable out = readBooleanArray(rexp, exec);
                return new BufferedDataTable[]{out};
            }
            case REXP.XT_ARRAY_INT : {
                BufferedDataTable out = readIntArray(rexp, exec);
                return new BufferedDataTable[]{out};
            }
            case REXP.XT_BOOL : {
                BufferedDataTable out = readBoolean(rexp, exec);
                return new BufferedDataTable[]{out};         
            }
            case REXP.XT_DOUBLE : {
                BufferedDataTable out = readDouble(rexp, exec);
                return new BufferedDataTable[]{out};   
            }
            case REXP.XT_INT : {
                BufferedDataTable out = readInt(rexp, exec);
                return new BufferedDataTable[]{out};
            }
            case REXP.XT_STR : {                
                BufferedDataTable out = readString(rexp, exec);
                return new BufferedDataTable[]{out};   
            }
            case REXP.XT_SYM : {
                BufferedDataTable out = readString(rexp, exec);
                return new BufferedDataTable[]{out}; 
            }
            case REXP.XT_UNKNOWN : {
                return new BufferedDataTable[]{null};
            }
            case REXP.XT_VECTOR : {
                BufferedDataTable out = readVector(rexp, exec);
                return new BufferedDataTable[]{out};
            }
            case REXP.XT_NULL : {
                return new BufferedDataTable[]{null};
            }
            case REXP.XT_ARRAY_DOUBLE : {
                BufferedDataTable out = readDoubleArray(rexp, exec);
                return new BufferedDataTable[]{out};
            }
            default: {
                throw new IllegalArgumentException("Unsupported type: " + rexp);
            }
        }
    }
    
    private BufferedDataTable readString(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        String matrix = rexp.asString();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataRow row = new DefaultRow(new StringCell("Row1"), matrix);
        DataTable table = new DefaultTable(new DataRow[]{row}, 
                new DataTableSpec(cspec));
        return exec.createBufferedDataTable(table, exec);
    }
    
    private BufferedDataTable readDouble(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        double matrix = rexp.asDouble();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", DoubleCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataRow row = new DefaultRow(new StringCell("Row1"), new double[]{matrix});
        DataTable table = new DefaultTable(new DataRow[]{row}, 
                new DataTableSpec(cspec));
        return exec.createBufferedDataTable(table, exec);
    }
    
    private BufferedDataTable readInt(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        int matrix = rexp.asInt();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", IntCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataRow row = new DefaultRow(new StringCell("Row1"), new int[]{matrix});
        DataTable table = new DefaultTable(new DataRow[]{row}, 
                new DataTableSpec(cspec));
        return exec.createBufferedDataTable(table, exec);
    }
    
    private BufferedDataTable readBoolean(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        RBool matrix = rexp.asBool();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataRow row = new DefaultRow(new StringCell("Row1"), matrix.toString());
        DataTable table = new DefaultTable(new DataRow[]{row}, 
                new DataTableSpec(cspec));
        return exec.createBufferedDataTable(table, exec);
    }
    
    private BufferedDataTable readBooleanArray(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        RBool[] matrix = (RBool[]) rexp.getContent();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataContainer dc = new DataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    matrix[i].toString()));
        }
        dc.close();
        return exec.createBufferedDataTable(dc.getTable(), exec);
    }

    private BufferedDataTable readDoubleArray(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        double[] matrix = rexp.asDoubleArray();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", DoubleCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataContainer dc = new DataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    matrix[i]));
        }
        dc.close();
        return exec.createBufferedDataTable(dc.getTable(), exec);
    }
    
    private BufferedDataTable readVector(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        Vector matrix = rexp.asVector();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", 
                        StringCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataContainer dc = new DataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.size(); i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.size());
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    ((REXP) matrix.get(i)).asString()));
        }
        dc.close();
        return exec.createBufferedDataTable(dc.getTable(), exec);
    }
    
    private BufferedDataTable readIntArray(
            final REXP rexp, final ExecutionContext exec) 
            throws CanceledExecutionException {
        int[] matrix = rexp.asIntArray();
        DataColumnSpec[] cspec = new DataColumnSpec[1];
        for (int i = 0; i < cspec.length; i++) {
            DataColumnSpecCreator colspeccreator = 
                new DataColumnSpecCreator("R", IntCell.TYPE);
            cspec[i] = colspeccreator.createSpec();
        }
        DataContainer dc = new DataContainer(new DataTableSpec(cspec));
        for (int i = 0; i < matrix.length; i++) {
            exec.checkCanceled();
            exec.setProgress(1.0 * i / matrix.length);
            dc.addRowToTable(new DefaultRow(new StringCell("Row" + (i + 1)),
                    new int[]{matrix[i]}));
        }
        dc.close();
        return exec.createBufferedDataTable(dc.getTable(), exec);
    }



    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        testExpressions(m_expression);
        checkRconnection();
        return new DataTableSpec[1];
    }

    private void testExpressions(final String[] rexps)
            throws InvalidSettingsException {
        for (int i = 0; i < rexps.length; i++) {
            String test = rexps[i].replace(" ", "");
            if (test.startsWith("R<-")) {
                // ok, we have an result in R
                return;
            }
        }
        throw new InvalidSettingsException("The result has to be provided"
                + " inside the variable R");
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addStringArray("EXPRESSION", m_expression);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_expression = settings.getStringArray("EXPRESSION");
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        testExpressions(settings.getStringArray("EXPRESSION"));
    }
    
    /**
     * @see org.knime.core.node.NodeModel#loadInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(
     * File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }
    
}
