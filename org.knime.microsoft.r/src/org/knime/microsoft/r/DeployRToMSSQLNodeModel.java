/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.microsoft.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;
import org.knime.r.FlowVariableRepository;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetNodeModel;
import org.knime.r.RSnippetSettings;
import org.knime.r.controller.RController;
import org.rosuda.REngine.REXP;

/**
 * Node Model which allows executing R code in a Microsoft SQL Server
 *
 * <h1>General process:</h1>
 * <ul>
 * <li>A R script which serializes knime.model and knime.flow.in using <code>saveRDS</code> is run</li>
 * <li>The binary data is retrieved and uploaded to an SQL table as a BLOB</li>
 * <li>The users script is prefixed with code which deserializes the data from the table to provide it as variables to
 * the user</li>
 * <li>The user R script retrieves the username and password to log into the database from the serverside R
 * instance</li>
 * <li>knime.out is output into a SQL table using <code>RxDataStep</code>, which is a special Microsoft R (RevoScaleR)
 * function only available with MS R</li>
 * </ul>
 *
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 *
 */
final class DeployRToMSSQLNodeModel extends RSnippetNodeModel {

    static final RSnippetNodeConfig RSNIPPET_NODE_CONFIG = new RSnippetNodeConfig() {
        @Override
        public Collection<PortType> getInPortTypes() {
            return Arrays.asList(DatabaseConnectionPortObject.TYPE, RPortObject.TYPE);
        }

        @Override
        protected Collection<PortType> getOutPortTypes() {
            return Arrays.asList(DatabaseConnectionPortObject.TYPE);
        }

        @Override
        protected String getScriptPrefix() {
            // This script prefix only has influence on the serialization code
            return "";
        }

        @Override
        protected String getDefaultScript() {
            // This default script only has influence on the sql R code
            return "knime.out<-cbind(knime.in, predict(knime.model, knime.in))";
        }

        @Override
        protected String getScriptSuffix() {
            // This script suffix only has influence on the serialization code
            return "";
        }

    };

    private static final NodeLogger LOGGER = NodeLogger.getLogger("Deploy R To MSSQL");

    private DeployRToMSSQLNodeSettings m_settings = new DeployRToMSSQLNodeSettings();

    /**
     * Constructor
     */
    public DeployRToMSSQLNodeModel() {
        super(RSNIPPET_NODE_CONFIG);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);

        assert inSpecs[0] instanceof DatabaseConnectionPortObjectSpec;
        assert inSpecs[1] instanceof RPortObjectSpec;

        final DatabaseConnectionPortObjectSpec databasePort = (DatabaseConnectionPortObjectSpec)inSpecs[0];

        /* Check if we are connected to a MSSQL database, since we require support for sq_execute_external_script */
        final DatabaseConnectionSettings databaseSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());

        if (!databaseSettings.getDriver().startsWith("com.microsoft")) {
            LOGGER.error("Deploy R To MSSQL Node does not support " + databasePort.getDatabaseIdentifier()
                + ".\nMake sure to use the Microsoft database connector and the Microsoft JDBC driver.");
            throw new InvalidSettingsException("This node only works with Microsoft SQL Server 2016+");
        }

        return new PortObjectSpec[]{new DatabaseConnectionPortObjectSpec(databaseSettings)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DatabaseConnectionPortObject databasePort = (DatabaseConnectionPortObject)inData[0];

        final DatabaseConnectionSettings connectionSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());
        final Connection connection = connectionSettings.createConnection(getCredentialsProvider());

        /* Serialize R Model */
        final RSnippetSettings settings = getRSnippet().getSettings();
        getRSnippet().getSettings().loadSettings(settings);

        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());

        final RController controller = new RController();
        controller.setUseNodeContext(true);
        final Blob blob = connection.createBlob();
        try {
            exec.checkCanceled();
            final String serializeScript =
                "conn<-rawConnection(raw(0),open='w');saveRDS(list(knime.model=knime.model,knime.flow.in=knime.flow.in),file=conn);"
                    + "knime.model.serialized<-rawConnectionValue(conn);close(conn);rm(conn)";
            executeSnippet(controller, serializeScript, inData, flowVarRepo, exec);

            final REXP serializedModelREXP = controller.eval("knime.model.serialized", true);

            final byte[] serializedModel = serializedModelREXP.asBytes();

            /* Put the serialized Model into a new "KNIME_R_WORKSPACE" table */
            connection.createStatement()
                .execute("IF OBJECT_ID('KNIME_R_WORKSPACE', 'U') IS NOT NULL DROP TABLE KNIME_R_WORKSPACE");
            connection.createStatement().execute("CREATE TABLE KNIME_R_WORKSPACE (workspace varbinary(MAX))");

            /* Send the serialized model as BLOB */
            final PreparedStatement stmt = connection.prepareStatement("INSERT INTO KNIME_R_WORKSPACE VALUES (?)");
            blob.setBytes(1, serializedModel);
            stmt.setBlob(1, blob);
            stmt.execute();

            pushFlowVariables(flowVarRepo);
        } finally {
            controller.close();
            blob.free();
        }

        /* Execute R code via SQL statement */
        final String rCode = getRSnippet().getDocument().getText(0, getRSnippet().getDocument().getLength()).trim();
        final String inTable = m_settings.getInputTableName();
        final String outTable = m_settings.getOutputTableName();
        final String query = getRunRCodeQuery().replace("${userRCode}", rCode.replace("'", "\""))
            .replace("${usr}", connectionSettings.getUserName(getCredentialsProvider()))
            .replace("${pwd}", connectionSettings.getPassword(getCredentialsProvider()))
            .replace("${inTableName}", inTable).replace("${outTableName}", outTable);

        LOGGER.debug("Running SQL R query with input table \"" + inTable + "\" and output table \"" + outTable + "\".");

        try {
            if (!connection.createStatement().execute(query)) {
                throw new RuntimeException("SQL Query failed.");
            }
        } catch (Exception e) {
            throw new RuntimeException("SQL Query failed.", e);
        }

        return new PortObject[]{new DatabaseConnectionPortObject(new DatabasePortObjectSpec(new DataTableSpec(),
            new DatabaseQueryConnectionSettings(connectionSettings, query)))};
    }

    /**
     * @return Code loaded from resource to execute R code in an SQL query.
     */
    private String getRunRCodeQuery() {
        try (final InputStream stream = getClass().getResource("RunRCode.sql").openStream()) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            return String.join("\n", reader.lines().collect(Collectors.toList()));
        } catch (IOException e) {
            throw new RuntimeException("CODING ERROR: Could not read RCodeQuery.sql", e);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_settings.saveSettingsTo(settings);
    }

}
