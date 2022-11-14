/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.r.FlowVariableRepository;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetNodeModel;
import org.knime.r.RSnippetSettings;
import org.knime.r.controller.ConsoleLikeRExecutor;
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
@Deprecated
class RunRInMSSQLNodeModel extends RSnippetNodeModel {

    interface TableChecker {
        boolean tableExists(String tableName) throws Exception;
    }
    private final static String DESERIALIZE_KNIME_VARS = //
        "knime.tmp<-readRDS(rawConnection(knimeserialized,open=\"r\"))\n" //
            + "if ('knime.model' %in% names(knime.tmp)) {\n" //
            + "    knime.model<-knime.tmp$knime.model\n" //
            + "}\n" //
            + "knime.tmp.script<-knime.tmp$knime.tmp.script\n" //
            + "knime.flow.in<-knime.tmp$knime.flow.in\n" //
            + "rm(knime.tmp,knimeserialized)\n";

    private static StringBuilder getRCodePrefix(final String user, final String pwd) {
        return new StringBuilder() //
            .append("knime.db.server<-knimedbserver\n" //
                + "knime.db.name<-knimedbname\n" //
                + "rm(knimedbserver,knimedbname)\n" //
                + "knime.db.connection<-paste('Driver=SQL Server;Server=', knime.db.server, ';Database=', knime.db.name, ';uid=")
            .append(user).append(";pwd=").append(pwd).append("\", sep='')\n");
    }

    private static StringBuilder getRCodeSuffix(final String outTableName) {
        return new StringBuilder() //
            .append("outputTable <- RxOdbcData(connectionString=knime.db.connection, table='").append(outTableName)
            .append("')\n") //
            .append("rxDataStep(inData=knime.out, outFile=outputTable, overwrite=TRUE)\n");
    }

    static final RSnippetNodeConfig RSNIPPET_NODE_CONFIG = new RSnippetNodeConfig() {
        @Override
        public Collection<PortType> getInPortTypes() {
            return Arrays.asList(PortObject.TYPE_OPTIONAL, DatabasePortObject.TYPE);
        }

        @Override
        protected Collection<PortType> getOutPortTypes() {
            return Arrays.asList(DatabasePortObject.TYPE);
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

    public static final int PORT_INDEX_R = 0;

    public static final int PORT_INDEX_DB = 1;

    private final RunRInMSSQLNodeSettings m_settings = new RunRInMSSQLNodeSettings();

    /**
     * Constructor
     */
    public RunRInMSSQLNodeModel() {
        this(RSNIPPET_NODE_CONFIG);
    }

    /**
     * @param config R Snippet Node Config
     */
    protected RunRInMSSQLNodeModel(final RSnippetNodeConfig config) {
        super(config);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);
        checkInputSpec(inSpecs);
        return new PortObjectSpec[]{null};
    }

    /**
     * @param inSpecs The input {@link PortObjectSpec}s
     * @throws InvalidSettingsException If the input spec is not valid
     */
    protected void checkInputSpec(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        assert inSpecs[PORT_INDEX_DB] instanceof DatabasePortObjectSpec;
        final DatabasePortObjectSpec databasePort = (DatabasePortObjectSpec)inSpecs[PORT_INDEX_DB];

        /* Check if we are connected to a MSSQL database, since we require support for sq_execute_external_script */
        final DatabaseQueryConnectionSettings databaseSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());

        if (!databaseSettings.getDriver().startsWith("com.microsoft")) {
            getLogger().error("Deploy R To MSSQL Node does not support " + databasePort.getDatabaseIdentifier()
                + ".\nMake sure to use the Microsoft database connector and the Microsoft JDBC driver.");
            throw new InvalidSettingsException("This node only works with Microsoft SQL Server 2016+");
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DatabasePortObject databasePort = (DatabasePortObject)inData[PORT_INDEX_DB];

        final DatabaseQueryConnectionSettings connectionSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());
        final Connection connection = connectionSettings.createConnection(getCredentialsProvider());
        final DatabaseUtility utility = DatabaseUtility.getUtility(connectionSettings.getDatabaseIdentifier());


        final String outputQuery = runRScript(exec, connection, inData, connectionSettings.getQuery(),
            connectionSettings.getUserName(getCredentialsProvider()),
            connectionSettings.getPassword(getCredentialsProvider()),
            tableName -> utility.tableExists(connection, tableName));


        final DatabaseQueryConnectionSettings querySettings =
            new DatabaseQueryConnectionSettings(connectionSettings, outputQuery);

        final Statement outputStatement = connection.createStatement();
        try (final ResultSet result = outputStatement.executeQuery(outputQuery)) {
            final DBReader conn = utility.getReader(querySettings);
            final DataTableSpec dbSpec = conn.getDataTableSpec(getCredentialsProvider());

            return new PortObject[]{new DatabasePortObject(new DatabasePortObjectSpec(dbSpec, querySettings))};
        } catch (SQLException ex) {
            Throwable cause = ExceptionUtils.getRootCause(ex);
            cause = ObjectUtils.defaultIfNull(cause, ex);
            throw new InvalidSettingsException("Error while validating SQL query: " + cause.getMessage(), ex);
        }
    }

    protected String runRScript(final ExecutionContext exec, final Connection connection, final PortObject[] inData,
        final String inputQuery, final String user, final String pwd, final TableChecker checker)
                throws Exception {

        /* Serialize R Model */
        final RSnippetSettings settings = getRSnippet().getSettings();
        getRSnippet().getSettings().loadSettings(settings);

        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());

        final String outputTable = m_settings.getOutputTableName();
        if (!m_settings.getOverwriteOutputTable()) {
            /* Check if output table already exists */
            if (checker.tableExists(outputTable)) {
                throw new RuntimeException("Output table already exists.");
            }
        }

        /* Generate R code to be run as SQL statement */
        final String rCode = getRSnippet().getDocument().getText(0, getRSnippet().getDocument().getLength()).trim();

        final StringBuilder userScript = new StringBuilder();
        userScript.append(rCode);
        userScript.append("\n");

        // Append R code that uploads result data to output table using RevoScaleR
        userScript.append(getRCodeSuffix(outputTable));

        String uniqueTableIdentifier = null;
        Blob blob = null;

        try (final RController controller = new RController(true)) {
            controller.initialize();

            exec.checkCanceled();

            exec.setProgress(0.0, "Importing data");
            /*
             * Serialize R variables on a local R instance through Rserve
             */
            controller.assign("knime.r.code", userScript.toString()); // send user script for serialization

            final String serializeScript = "conn<-rawConnection(raw(0),open='w')\n" // capture output of saveRDS into a binary vector
                + "knime.varstosend<-list(knime.flow.in=knime.flow.in,knime.tmp.script=knime.r.code)\n" // Always send knime.flow.in
                + "if(exists('knime.model')){knime.varstosend$knime.model<-knime.model}\n" // serialize knime.model only if it exists
                + "saveRDS(knime.varstosend,file=conn)\n" // serialize the list of variables
                + "knime.serialized<-rawConnectionValue(conn)\n" //
                + "close(conn);rm(conn,knime.varstosend)\n"; // remove unneeded variables

            // Execute above R code in the workspace at the input port.
            executeSnippet(controller, serializeScript, inData, flowVarRepo, exec.createSubExecutionContext(0.33));

            // Get result of the serialization script
            final REXP serializedModelREXP = controller.eval("knime.serialized", true);
            final byte[] serializedModel = serializedModelREXP.asBytes();
            exec.checkCanceled();

            /*
             * Put the serialized Model into a new "KNIME_R_WORKSPACE" table using JDBC
             */
            // Find a unique identifier for the table to contain our serialized variables
            exec.setProgress(0.4, "Creating table for R workspace");
            synchronized (RunRInMSSQLNodeModel.class) {
                int i = 0; // never allowed to be negative, otherwise invalid identifier
                do {
                    uniqueTableIdentifier = "KNIME_R_WORKSPACE" + (i++);
                } while (checker.tableExists(uniqueTableIdentifier));

                // Create the table with found identifier
                connection.createStatement()
                    .execute("CREATE TABLE [" + uniqueTableIdentifier + "] (workspace varbinary(MAX))");
            }
            exec.checkCanceled();

            // Send the serialized model as BLOB
            exec.setProgress(0.5, "Sending data to SQL");
            final PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO [" + uniqueTableIdentifier + "] VALUES (?)");
            blob = connection.createBlob();
            blob.setBytes(1, serializedModel);
            stmt.setBlob(1, blob);
            stmt.execute();

            pushFlowVariables(flowVarRepo);
            exec.checkCanceled();

            /* Run R on MSSQL server embedded in a SQL Statement.
             * This script handles a couple of things:
             *  - Deserialization of the variables contained in the blob we uploaded into the KNIME_R_WORKSPACE* table
             *  - Capturing output of the user script
             *  - Running the user script with manual parse() and eval() to catch user script errors
             */
            exec.setProgress(0.6, "Executing R script on Microsoft SQL Server");
            final StringBuilder embeddedRScript = new StringBuilder();
            embeddedRScript.append(getRCodePrefix(user, pwd));

            // start output capturing
            embeddedRScript.append(ConsoleLikeRExecutor.CAPTURE_OUTPUT_PREFIX);
            embeddedRScript.append("\n");

            // deserialize variables
            embeddedRScript.append(DESERIALIZE_KNIME_VARS);
            embeddedRScript.append("\n");

            // execute user script in knime.r.script/knime.tmp.script
            embeddedRScript.append(ConsoleLikeRExecutor.CODE_EXECUTION);
            embeddedRScript.append("\n");

            // Create a data frame from the captured output and return as result set
            embeddedRScript.append(ConsoleLikeRExecutor.CAPTURE_OUTPUT_POSTFIX);
            embeddedRScript.append(";knime.out<-data.frame(knime.output.ret)");

            final String query = getRunRCodeQuery() //
                .replace("${RCode}", embeddedRScript.toString().replaceAll("'", "\"")) //
                .replace("${KnimeRWorkspaceTable}", uniqueTableIdentifier) //
                .replace("${inputQuery}", inputQuery);

            getLogger().debugWithFormat("Running SQL R query with input query \"%s\" and output table \"%s\".",
                inputQuery, outputTable);

            exec.setProgress(0.95, "Retrieving R console output and cleaning up");
            try (final ResultSet result = connection.createStatement().executeQuery(query)) {
                /* First returned row is captured output */
                result.next();
                getLogger().debug("MSSQL R Output: " + result.getString(1));

                /* First returned row is captured errors */
                result.next();
                final String errors = result.getString(1);
                if (errors != null) {
                    getLogger().error("MSSQL R Errors: " + errors);
                    throw new RuntimeException("Errors during R code execution: " + errors);
                }

                /* Ignore all other rows (should be none) */
                while (result.next()) {
                }
            }
        } finally {
            if (uniqueTableIdentifier != null) {
                try {
                    // Delete the table containing the serialized R variables
                    connection.createStatement().execute("IF OBJECT_ID('" + uniqueTableIdentifier
                        + "', 'U') IS NOT NULL DROP TABLE [" + uniqueTableIdentifier + "]");
                } catch (SQLException e) {
                    throw new RuntimeException(
                        "Unable to remove temporary table '" + uniqueTableIdentifier + "'. Please manually remove it.",
                        e);
                }
            }

            if (blob != null) {
                blob.free();
            }
        }

        exec.setProgress(0.98, "Getting output table specification");
        /* Build output query and get table spec */
        /* On MSSQL we can use [ ] to specify an identifier */
        final String outputQuery = "SELECT * FROM [" + outputTable + "]";
        return outputQuery;
    }

    /**
     * @return Code loaded from resource to execute R code in an SQL query.
     */
    private String getRunRCodeQuery() {
        try (final BufferedReader reader =
            new BufferedReader(new InputStreamReader(getClass().getResource("RunRCode.sql").openStream()))) {
            return String.join("\n", reader.lines().collect(Collectors.toList()));
        } catch (final IOException e) {
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
