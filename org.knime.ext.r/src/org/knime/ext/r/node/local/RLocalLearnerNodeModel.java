/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
 *   12.09.2008 (gabriel): created
 */
package org.knime.ext.r.node.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.ext.r.node.RConsoleModel;
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
public class RLocalLearnerNodeModel extends RAbstractLocalNodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RLocalLearnerNodeModel.class);

    private String m_rCommand = RDialogPanel.DEFAULT_R_COMMAND;

    /**
     *
     */
    public RLocalLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{RPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        checkRExecutable();
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }

    /**
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
        RPortObject out;

        try {
            // write data to csv
            inDataCsvFile = writeInDataCsvFile(
                    (BufferedDataTable)inDataTables[0], exec);

            // execute R cmd
            StringBuilder completeCmd = new StringBuilder();
            completeCmd.append(SET_WORKINGDIR_CMD);
            completeCmd.append(READ_DATA_CMD_PREFIX);
            completeCmd.append(inDataCsvFile.getAbsolutePath().replace('\\',
                    '/'));
            completeCmd.append(READ_DATA_CMD_SUFFIX);

            completeCmd.append(m_rCommand.trim());
            completeCmd.append("\n");

            File fileR = File.createTempFile("~knime", ".R");
            fileR.deleteOnExit();
            completeCmd.append(WRITE_MODEL_CMD_PREFIX);
            completeCmd.append(fileR.getAbsolutePath().replace('\\', '/'));
            completeCmd.append(WRITE_MODEL_CMD_SUFFIX);

            // write R command
            String rCmd = completeCmd.toString();
            LOGGER.debug("R Command: \n" + rCmd);
            rCommandFile = writeRcommandFile(rCmd);
            rOutFile = new File(rCommandFile.getAbsolutePath() + ".Rout");

            // create shell command
            StringBuilder shellCmd = new StringBuilder();

            final String rBinaryFile = getRBinaryPath();
            shellCmd.append(rBinaryFile);

            shellCmd.append(" CMD BATCH ");
            shellCmd.append("\"" + rCommandFile.getAbsolutePath() + "\"");
            shellCmd.append(" \"" + rOutFile.getAbsolutePath() + "\"");

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
            out = new RPortObject(fileR);
        } finally {
            // delete all temp files
            deleteFile(inDataCsvFile);
            deleteFile(tempOutData);
            deleteFile(rCommandFile);
            deleteFile(rOutFile);
        }
        return new PortObject[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_rCommand = RDialogPanel.getExpressionFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        RDialogPanel.setExpressionTo(settings, m_rCommand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        String exp = RDialogPanel.getExpressionFrom(settings);
        RConsoleModel.testExpressions(exp.split("\n"));
    }

}
