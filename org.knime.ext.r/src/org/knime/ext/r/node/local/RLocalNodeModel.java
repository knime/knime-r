/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.09.2007 (gabriel): created
 */
package org.knime.ext.r.node.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.base.node.util.exttool.CommandExecution;
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
import org.knime.core.node.PortObject;
import org.knime.core.node.PortType;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.preferences.RPreferenceInitializer;

/**
 * <code>RLocalNodeModel</code> is an abstract
 * <code>StdOutBufferedNodeModel</code> which can be extended to implement a R
 * node using a local R installation. This abstract node model provides
 * functionality to write incoming data of a <code>DataTable</code> into a csv
 * file and read this data into R. The R variable containing the data is named
 * "R". Additional R commands to modify and process this data can be specified
 * by a class extending <code>RLocalNodeModel</code>, by implementing the
 * abstract {@link RLocalNodeModel#getCommand()} method. To access i.e. the
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
 * <br/>
 * Additionally this class provides a preprocessing method
 * {@link RLocalNodeModel#preprocessDataTable(PortObject[], ExecutionContext)}
 * which can be overwritten to preprocess that input data, as well as a
 * postprocess method
 * {@link RLocalNodeModel#postprocessDataTable(
 * BufferedDataTable[], ExecutionContext)}
 * which can be overwritten to process that data after the R script execution.
 * If these methods are not overwritten the data will not be modified.
 *
 * @author Kilian Thiel, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class RLocalNodeModel extends ExtToolOutputNodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RLocalNodeModel.class);

    /**
     * The temp directory used to save csv, script R output files temporarily.
     */
    protected static final String TEMP_PATH =
        System.getProperty("java.io.tmpdir").replace('\\', '/');


    /**
     * The delimiter used for creation of csv files.
     */
    protected static final String DELIMITER = ",";

    private final SettingsModelString m_rbinaryFileSettingsModel =
        RLocalNodeDialogPane.createRBinaryFile();

    private final SettingsModelBoolean m_useSpecifiedModel =
        RLocalNodeDialogPane.createUseSpecifiedFileModel();

    /** R commands to set working dir, write and reads csv files. */
    private final String m_setWorkingDirCmd =
        "setwd(\"" + TEMP_PATH + "\");\n";

    private static final String READ_DATA_CMD_PREFIX = "R <- read.csv(\"";

    private static final String READ_DATA_CMD_SUFFIX = "\", header = TRUE);\n";

    private static final String WRITE_DATA_CMD_PREFIX = "write.csv(R, \"";

    private static final String WRITE_DATA_CMD_SUFFIX =
        "\", row.names = FALSE);\n";

    /**
     * Constructor of <code>RLocalNodeModel</code> creating a model with one
     * data in port an one data out port.
     */
    public RLocalNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Constructor of <code>RLocalNodeModel</code> creating a model with one
     * data in port and one data out port if and only if <code>hasOutput</code>
     * is set <code>true</code>. Otherwise the node will not have any
     * data out port.
     *
     * @param hasOutput If set <code>true</code> the node is instantiated
     * with one data out port if <code>false</code> with none.
     */
    public RLocalNodeModel(final boolean hasOutput) {
        super(new PortType[]{BufferedDataTable.TYPE},
                numberOfOuts(hasOutput));
    }

    private static PortType[] numberOfOuts(final boolean hasOutput) {
        if (hasOutput) {
            return new PortType[]{BufferedDataTable.TYPE};
        }
        return new PortType[0];
    }

    /**
     * Implement this method to specify certain R code to run. Be aware that
     * this R code has to be valid, otherwise the node will not execute
     * properly. To access the input data of the node via R use the variable
     * "R". To access i.e. the first three columns of a table and reference
     * them by another variable "a" the R command "a <- R[1:3];" can be used.
     * End all R command lines with a semicolon and a line break. The data
     * which has to be returned be the node as out data has to be stored in
     * the "R" variable again, so take care to reference your data by "R".
     *
     * @return The R command to execute.
     */
    protected abstract String getCommand();

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

    /**
     * First the
     * {@link RLocalNodeModel#preprocessDataTable(PortObject[],
     * ExecutionContext)}
     * method is called to preprocess that input data. Further a csv file is
     * written containing the input data. Next a R script is created consisting
     * of R commands to import the data of the csv file, the R code specified in
     * the command string and the export of the modified data into a output
     * table. This table is returned at the end.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        // preprocess data in in DataTable.
        PortObject[] inDataTables = preprocessDataTable(inData, exec);

        File tempOutData = null;
        File inDataCsvFile = null;
        File rCommandFile = null;
        File rOutFile = null;
        BufferedDataTable[] dts = null;

        try {
            // write data to csv
            inDataCsvFile = writeInDataCsvFile(
                    (BufferedDataTable)inDataTables[0], exec);

            // execute R cmd
            StringBuilder completeCmd = new StringBuilder();
            completeCmd.append(m_setWorkingDirCmd);
            completeCmd.append(READ_DATA_CMD_PREFIX);
            completeCmd.append(inDataCsvFile.getAbsolutePath().replace('\\',
                    '/'));
            completeCmd.append(READ_DATA_CMD_SUFFIX);

            completeCmd.append(getCommand().trim());
            completeCmd.append("\n");

            tempOutData = File.createTempFile("R-outDataTempFile-", ".csv",
                    new File(TEMP_PATH));
            completeCmd.append(WRITE_DATA_CMD_PREFIX);
            completeCmd
                    .append(tempOutData.getAbsolutePath().replace('\\', '/'));
            completeCmd.append(WRITE_DATA_CMD_SUFFIX);

            // write R command
            String rCmd = completeCmd.toString();
            LOGGER.debug("R command: \n" + rCmd);
            rCommandFile = writeRcommandFile(rCmd);
            rOutFile = new File(rCommandFile.getAbsolutePath() + ".Rout");

            // create shell command
            StringBuilder shellCmd = new StringBuilder();

            String rBinaryFile = RPreferenceInitializer.getRPath();
            if (m_useSpecifiedModel.isEnabled()) {
                rBinaryFile = m_rbinaryFileSettingsModel.getStringValue();
            }
            shellCmd.append(rBinaryFile);

            shellCmd.append(" CMD BATCH ");
            shellCmd.append(rCommandFile.getAbsolutePath());
            shellCmd.append(" " + rOutFile.getAbsolutePath());

            // execute shell command
            String shcmd = shellCmd.toString();
            LOGGER.debug("Shell command: \n" + shcmd);

            CommandExecution cmdExec = new CommandExecution(shcmd);
            cmdExec.addObserver(this);
            int exitVal = cmdExec.execute(exec);

            setExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
            setExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));

            if (exitVal != 0) {
                String rErr = "";

                // before we return, we save the output in the failing list
                synchronized (cmdExec) {
                    setFailedExternalOutput(new LinkedList<String>(cmdExec
                            .getStdOutput()));
                }
                synchronized (cmdExec) {

                    // save error description of the Rout file to the ErrorOut
                    LinkedList<String> list = new LinkedList<String>(
                            cmdExec.getStdErr());

                    list.add("#############################################");
                    list.add("#");
                    list.add("# Content of .Rout file: ");
                    list.add("#");
                    list.add("#############################################");
                    list.add(" ");
                    BufferedReader bfr = new BufferedReader(
                            new FileReader(rOutFile));
                    String line;
                    while ((line = bfr.readLine()) != null) {
                        list.add(line);
                    }
                    bfr.close();

                    // use row before last as R error.
                    int index = list.size() - 2;
                    if (index >= 0) {
                        rErr = list.get(index);
                    }
                    setFailedExternalErrorOutput(list);
                }

                LOGGER.debug("Execution of R Script failed with exit code: "
                        + exitVal);
                throw new IllegalStateException(
                        "Execution of R script failed: " + rErr);
            }

            // read data from R output csv into a buffered data table.
            ExecutionContext subExecCon = exec.createSubExecutionContext(1.0);
            BufferedDataTable dt = readOutData(tempOutData, subExecCon);

            // postprocess data in out DataTable.
            dts = postprocessDataTable(new BufferedDataTable[]{dt}, exec);

        } finally {
            // delete all temp files
            deleteFile(inDataCsvFile);
            deleteFile(tempOutData);
            deleteFile(rCommandFile);
            deleteFile(rOutFile);
        }

        // return this table
        return dts;
    }


    private boolean deleteFile(final File file) {
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

    private File writeRcommandFile(final String cmd) throws IOException {
        File tempCommandFile = File.createTempFile("R-inDataTempFile-", ".r",
                    new File(TEMP_PATH));
        FileWriter fw = new FileWriter(tempCommandFile);
        fw.write(cmd);
        fw.close();
        return tempCommandFile;
    }

    private File writeInDataCsvFile(final BufferedDataTable inData,
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

    private BufferedDataTable readOutData(final File outData,
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
        settings.setGlobalMissingValuePattern("NA");
        settings = FileAnalyzer.analyze(settings, null);

        DataTableSpec tSpec = settings.createDataTableSpec();

        FileTable fTable = new FileTable(tSpec, settings, settings
                    .getSkippedColumns(), exec);

        return exec.createBufferedDataTable(fTable, exec);
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

    private static void checkRExecutable(final String path)
            throws InvalidSettingsException {
        File binaryFile = new File(path);
        if (!binaryFile.exists() || !binaryFile.isFile()
                || !binaryFile.canExecute()) {
            throw new InvalidSettingsException("R Binary \""
                        + path + "\" not correctly specified.");
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
        checkRExecutable(m_rbinaryFileSettingsModel.getStringValue());
    }

}
