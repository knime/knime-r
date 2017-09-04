/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.ext.r.node.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.RDialogPanel;
import org.knime.ext.r.preferences.RPreferenceProvider;

/**
 * This is the model implementation of RLocalSource node. A general data reader node for R.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich, Switzerland
 * @since 2.8
 */
public class RLocalSourceNodeModel extends RAbstractLocalNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RLocalSourceNodeModel.class);

    private String m_rCommand = "R = read.csv(filename)";

    /**
     * Creates a new instance of <code>RLocalSourceNodeModel</code> with given in- and out-port specification.
     * 
     * @param pref provider for R executable
     */
    public RLocalSourceNodeModel(final RPreferenceProvider pref) {
        super(new PortType[]{}, new PortType[]{BufferedDataTable.TYPE}, pref);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        checkRExecutable();
        return new DataTableSpec[]{null};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inPorts, final ExecutionContext exec) throws Exception {

        File tempOutData = null;
        File rCommandFile = null;
        File rOutFile = null;
        BufferedDataTable[] dts = null;

        try {
            // execute R cmd
            final StringBuilder completeCmd = new StringBuilder();
            completeCmd.append(getSetWorkingDirCmd());

            // result data
            completeCmd.append(resolveVariablesInScript(m_rCommand.trim()));
            completeCmd.append("\n");

            // write result data to csv
            tempOutData = FileUtil.createTempFile("R-outDataTempFile-", ".csv", new File(m_tempPath), true);
            completeCmd.append(WRITE_DATA_CMD_PREFIX);
            completeCmd.append(tempOutData.getAbsolutePath().replace('\\', '/'));
            completeCmd.append(WRITE_DATA_CMD_SUFFIX);

            // write R command
            final String rCmd = completeCmd.toString();
            LOGGER.debug("R Command: \n" + rCmd);
            rCommandFile = writeRcommandFile(rCmd);
            rOutFile = new File(rCommandFile.getAbsolutePath() + ".Rout");

            // create shell command
            final StringBuilder shellCmd = new StringBuilder();

            final String rBinaryFile = getRBinaryPathAndArguments();
            shellCmd.append(rBinaryFile);
            shellCmd.append(" " + rCommandFile.getName());
            shellCmd.append(" " + rOutFile.getName());

            // execute shell command
            final String shcmd = shellCmd.toString();
            LOGGER.debug("Shell command: \n" + shcmd);

            final CommandExecution cmdExec = new CommandExecution(shcmd);
            cmdExec.addObserver(this);
            cmdExec.setExecutionDir(rCommandFile.getParentFile());
            final int exitVal = cmdExec.execute(exec);

            setExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
            setExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));

            String rErr = "";

            if (exitVal != 0) {
                // before we return, we save the output in the failing list
                synchronized (cmdExec) {
                    setFailedExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));
                }
            }
            synchronized (cmdExec) {

                // save error description of the Rout file to the ErrorOut
                final LinkedList<String> list = new LinkedList<String>(cmdExec.getStdErr());

                list.add("#############################################");
                list.add("#");
                list.add("# Content of .Rout file: ");
                list.add("#");
                list.add("#############################################");
                list.add(" ");
                final BufferedReader bfr = new BufferedReader(new FileReader(rOutFile));
                String line;
                while ((line = bfr.readLine()) != null) {
                    list.add(line);
                }
                bfr.close();

                // use row before last as R error.
                final int index = list.size() - 2;
                if (index >= 0) {
                    rErr = list.get(index);
                }

                if (exitVal != 0) {
                    setFailedExternalErrorOutput(list);
                    LOGGER.debug("Execution of R Script failed with exit code: " + exitVal);
                    throw new IllegalStateException("Execution of R script failed: " + rErr);
                } else {
                    setExternalOutput(list);
                }
            }

            // read data from R output csv into a buffered data table.
            final ExecutionContext subExecCon = exec.createSubExecutionContext(1.0);
            final BufferedDataTable dt = readOutData(tempOutData, subExecCon);

            // postprocess data in out DataTable.
            dts = postprocessDataTable(new BufferedDataTable[]{dt}, exec);

        } finally {
            // delete all temp files
            deleteFile(tempOutData);
            deleteFile(rCommandFile);
            deleteFile(rOutFile);
        }

        // return this table
        return dts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_rCommand = RDialogPanel.getExpressionFrom(settings, RDialogPanel.DEFAULT_R_COMMAND);
        try {
            m_argumentsR.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ise) {
            // load old workflow no option is used, overwrite new dialog dft
            m_argumentsR.setStringValue("");
        }
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
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        final String exp = RDialogPanel.getExpressionFrom(settings);
        if ((exp == null) || exp.trim().isEmpty()) {
            throw new InvalidSettingsException("Configure node and enter a non-empty R script.");
        }
    }

}
