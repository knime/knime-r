/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.09.2007 (gabriel): created
 */
package org.knime.ext.r.node.local;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * <code>RAbstractLocalNodeModel</code> is an abstract
 * <code>StdOutBufferedNodeModel</code> which can be extended to implement a R
 * node using a local R installation. This abstract node model provides
 * functionality to write incoming data of a <code>DataTable</code> into a csv
 * file and read this data into R. The R variable containing the data is named
 * "R". Additional R commands to modify and process this data can be specified
 * by a class extending <code>RAbstractLocalNodeModel</code>. To access i.e. the
 * first three columns of a table and reference them by another variable "a" the
 * R command "a <- R[1:3]" can be used.<br/>
 * Further, this class writes the data
 * referenced by the R variable "R" after execution of the additional commands
 * into a csv file and generates an outgoing <code>DataTable</code> out of it,
 * which is returned by this node. This means, the user has to take care that
 * the processed data have to be referenced by the variable "R".
 * <br />
 * Note that the number of input data tables is one and can not be modified when
 * extending this node model. The number of output data tables can be specified
 * but only one or zero will make any sense since only the data referenced by
 * the "R" variable will be exported as output data table if the number of
 * output tables is greater than zero.
 *
 * @author Kilian Thiel, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class RAbstractLocalNodeModel extends ExtToolOutputNodeModel {
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RAbstractLocalNodeModel.class);
    
    /**
     * The R expression prefix to read data. 
     */
    static final String READ_DATA_CMD_PREFIX = "R <- read.csv(\"";

    /**
     * The R expression suffix to read data.
     */
    static final String READ_DATA_CMD_SUFFIX = "\", header = TRUE);\n";

    /**
     * The R expression prefix to write data.
     */
    static final String WRITE_DATA_CMD_PREFIX = "write.csv(R, \"";

    /**
     * The R expression suffix to read data.
     */
    static final String WRITE_DATA_CMD_SUFFIX =
        "\", row.names = FALSE);\n";
    
    /**
     * The R expression prefix to write a model.
     */
    static final String WRITE_MODEL_CMD_PREFIX = "save(R, file=\"";

    /**
     * The R expression suffix to write a model.
     */
    static final String WRITE_MODEL_CMD_SUFFIX = "\", ascii=TRUE);\n";
    
    /**
     * The R expression prefix to load a model.
     */
    static final String LOAD_MODEL_CMD_PREFIX = "load(\"";
    
    /**
     * The R expression suffix to load a model.
     */
    static final String LOAD_MODEL_CMD_SUFFIX = "\");\n";

    /**
     * The temp directory used to save csv, script R output files temporarily.
     */
    static final String TEMP_PATH =
        System.getProperty("java.io.tmpdir").replace('\\', '/');
    
    /** R commands to set working dir, write and reads csv files. */
    static final String SET_WORKINGDIR_CMD =
        "setwd(\"" + TEMP_PATH + "\");\n";

    /** The delimiter used for creation of csv files. */
    private static final String DELIMITER = ",";

    /**
     * Model saving the path to the R binary file.
     */
    private final SettingsModelString m_rbinaryFileSettingsModel =
        RLocalNodeDialogPane.createRBinaryFile();

    /**
     * Model specifying if specific R binary file have to be used.
     */
    private final SettingsModelBoolean m_useSpecifiedModel =
        RLocalNodeDialogPane.createUseSpecifiedFileModel();

    /**
     * Constructor of <code>RAbstractLocalNodeModel</code> with given in- and
     * out-port specification.
     * 
     * @param inPorts in-port specification.
     * @param outPorts out-port specification.
     */
    protected RAbstractLocalNodeModel(final PortType[] inPorts,
            final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rbinaryFileSettingsModel.loadSettingsFrom(settings);
        m_useSpecifiedModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rbinaryFileSettingsModel.saveSettingsTo(settings);
        m_useSpecifiedModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rbinaryFileSettingsModel.validateSettings(settings);
        m_useSpecifiedModel.validateSettings(settings);
    }
    
    /**
     * The method enables one to preprocess the data before the execution
     * of the R command. This method is called before the R commands are
     * executed. All the processing which has to be done before has to be
     * implemented here, i.e. column filtering and so on.
     * This implementation is a dummy implementation which only passes
     * through the unmodified inData.
     *
     * @param inData The in data to preprocess.
     * @param exec To monitor the status of processing.
     * @return The preprocessed in data.
     * @throws Exception If other problems occur.
     */
    protected PortObject[] preprocessDataTable(
            final PortObject[] inData, final ExecutionContext exec)
            throws Exception {
        return inData;
    }

    /**
     * The method enables one to postprocess the data modified by the
     * execution of the R command. This method is called after the R commands
     * are executed. All the processing which has to be done after this have
     * to be implemented here.
     * This implementation is a dummy implementation which only passes
     * through the unmodified outData.
     *
     * @param outData The in data to postprocess.
     * @param exec To monitor the status of processing.
     * @return The postprocessed out data.
     * @throws Exception If other problems occur.
     */
    protected BufferedDataTable[] postprocessDataTable(
            final BufferedDataTable[] outData, final ExecutionContext exec)
            throws Exception {
        return outData;
    }

    private static void checkRExecutable(final String path)
            throws InvalidSettingsException {
        if (path == null || path.trim().length() == 0) {
            throw new InvalidSettingsException("R Binary not specified.");
        }
        File binaryFile = new File(path);
        if (!binaryFile.exists()) {
            throw new InvalidSettingsException("R Binary \""
                        + path + "\" not found.");
        }
        if (!binaryFile.isFile() || !binaryFile.canExecute()) {
            throw new InvalidSettingsException("R Binary \""
                        + path + "\" not executable.");
        }        
    }

    /**
     * Checks if R executable exists and is a file, otherwise an exception will
     * be thrown.
     *
     * @throws InvalidSettingsException If the R executable is not a valid file
     * or does not exist.
     */
    protected void checkRExecutable() throws InvalidSettingsException {
        if (!m_useSpecifiedModel.getBooleanValue()) {
            m_rbinaryFileSettingsModel.setStringValue(
                    RPreferenceInitializer.getRPath());
        }
        checkRExecutable(m_rbinaryFileSettingsModel.getStringValue());
    }
    

    /**
     * Deletes the specified file. If the file is a directory the directory
     * itself as well as its files and subdirectories are deleted.
     * 
     * @param file The file to delete.
     * @return <code>true</code> if the file could be deleted, otherwise 
     * <code>false</code>.
     */
    static boolean deleteFile(final File file) {
        boolean del = false;
        if (file != null && file.exists()) {
            del = FileUtil.deleteRecursively(file);

            // if file could not be deleted call GC and try again
            if (!del) {
                // !!! What a mess !!!
                // It is possible that there are still open streams around
                // holding the file. Therefore these streams, actually belonging
                // to the garbage, has to be collected by the GC.
                System.gc();

                // try to delete again ....
                del = FileUtil.deleteRecursively(file);
                if (!del) {
                    // ok that's it no trials anymore ...
                    LOGGER.debug(file.getAbsoluteFile()
                            + " could not be deleted !");
                }
            }
        }
        return del;
    }

    /**
     * Writes the given string into a file and returns it.
     * 
     * @param cmd The string to write into a file.
     * @return The file containing the given string.
     * @throws IOException If string could not be written to a file.
     */
    static File writeRcommandFile(final String cmd) throws IOException {
        File tempCommandFile = File.createTempFile("R-inDataTempFile-", ".r",
                    new File(TEMP_PATH));
        FileWriter fw = new FileWriter(tempCommandFile);
        fw.write(cmd);
        fw.close();
        return tempCommandFile;
    }

    /**
     * Writes the data contained in the given data table into a file as csv
     * format.
     * 
     * @param inData The data table containing the data to write.
     * @param exec The execution context to enable the user to cancel the 
     * process.
     * @return The csv file with the data.
     * @throws IOException If the data could not be written into the file.
     * @throws CanceledExecutionException If user canceled the process.
     */
    static File writeInDataCsvFile(final BufferedDataTable inData,
            final ExecutionContext exec) throws IOException,
            CanceledExecutionException {
        // create Temp file
        File tempInDataFile = File.createTempFile("R-inDataTempFile-", ".csv",
                    new File(TEMP_PATH));

        // write data to file
        FileWriter fw = new FileWriter(tempInDataFile);
        FileWriterSettings fws = new FileWriterSettings();
        fws.setColSeparator(DELIMITER);
        fws.setWriteColumnHeader(true);

        CSVWriter writer = new CSVWriter(fw, fws);

        BufferedDataTable newTable = exec.createSpecReplacerTable(inData,
               RDialogPanel.getRenamedDataTableSpec(inData.getDataTableSpec()));
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        writer.write(newTable, subExec);

        writer.close();
        return tempInDataFile;
    }

    /**
     * Reads data out of specified csv file and creates a data table.
     * 
     * @param outData The file containing the csv data.
     * @param exec The execution context.
     * @return The data table containing the data of the specified file.
     * @throws IOException If file could not be opened or read.
     * @throws CanceledExecutionException If user canceled the process.
     */
    static BufferedDataTable readOutData(final File outData,
            final ExecutionContext exec) throws IOException,
            CanceledExecutionException {
        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        settings.setDataFileLocationAndUpdateTableName(
                outData.toURI().toURL());
        settings.addDelimiterPattern(DELIMITER, false, false, false);
        settings.addRowDelimiter("\n", true);
        settings.addQuotePattern("\"", "\"");
        settings.setDelimiterUserSet(true);
        settings.setFileHasColumnHeaders(true);
        settings.setFileHasColumnHeadersUserSet(true);
        settings.setFileHasRowHeaders(false);
        settings.setFileHasRowHeadersUserSet(true);
        settings.setQuoteUserSet(true);
        settings.setWhiteSpaceUserSet(true);
        settings.setMissValuePatternStrCols("NA");
        settings = FileAnalyzer.analyze(settings, null);

        DataTableSpec tSpec = settings.createDataTableSpec();

        FileTable fTable = new FileTable(tSpec, settings, settings
                    .getSkippedColumns(), exec);

        return exec.createBufferedDataTable(fTable, exec);
    }
    
    /**
     * Path to R binary.
     * @return R binaray path
     */
    protected final String getRBinaryPath() {
        if (m_useSpecifiedModel.getBooleanValue()) {
            return m_rbinaryFileSettingsModel.getStringValue();
        } else {
            return RPreferenceInitializer.getRPath();
        }
    }

}
