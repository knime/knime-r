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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.database.DBDataObject;
import org.knime.database.SQLQuery;
import org.knime.database.agent.metadata.DBMetadataReader;
import org.knime.database.connection.ConnectionProvider;
import org.knime.database.connection.DBConnectionController;
import org.knime.database.connection.UserDBConnectionController;
import org.knime.database.datatype.mapping.DBTypeMappingRegistry;
import org.knime.database.datatype.mapping.DBTypeMappingService;
import org.knime.database.driver.DBDriverDefinition;
import org.knime.database.extension.mssql.MSSQLServer;
import org.knime.database.model.impl.DefaultDBTable;
import org.knime.database.port.DBDataPortObject;
import org.knime.database.port.DBDataPortObjectSpec;
import org.knime.database.session.DBSession;
import org.knime.datatype.mapping.DataTypeMappingConfiguration;
import org.knime.datatype.mapping.DataTypeMappingDirection;
import org.knime.r.RSnippetNodeConfig;

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
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 *
 */
final class RunRInMSSQLNodeModel2 extends RunRInMSSQLNodeModel {
    static final RSnippetNodeConfig RSNIPPET_NODE_CONFIG2 = new RSnippetNodeConfig() {
        @Override
        public Collection<PortType> getInPortTypes() {
            return Arrays.asList(PortObject.TYPE_OPTIONAL, DBDataPortObject.TYPE);
        }

        @Override
        protected Collection<PortType> getOutPortTypes() {
            return Arrays.asList(DBDataPortObject.TYPE);
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

    RunRInMSSQLNodeModel2() {
        super(RSNIPPET_NODE_CONFIG2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkInputSpec(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DBDataPortObjectSpec databasePort = (DBDataPortObjectSpec)inSpecs[PORT_INDEX_DB];
        /* Check if we are connected to a MSSQL database, since we require support for sq_execute_external_script */
        final DBSession session = databasePort.getDBSession();
        if (!session.getDBType().equals(MSSQLServer.DB_TYPE)) {
            getLogger().error("Make sure to use the Microsoft database connector.");
            throw new InvalidSettingsException("This node only works with Microsoft SQL Server 2016+");
        }
        final DBDriverDefinition driverDefinition = session.getDriver().getDriverDefinition();
        if (!driverDefinition.getDriverClass().startsWith("com.microsoft")) {
            getLogger().error("Deploy R To MSSQL Node does not support " + driverDefinition.getName()
                + ".\nMake sure to use the Microsoft JDBC driver.");
            throw new InvalidSettingsException("This node only works with Microsoft SQL Server 2016+");
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DBDataPortObject dbPort = (DBDataPortObject)inData[PORT_INDEX_DB];
        final DBSession session = dbPort.getDBSession();
        final DBTypeMappingService<?, ?> mappingService =
            DBTypeMappingRegistry.getInstance().getDBTypeMappingService(session.getDBType());
        final DataTypeMappingConfiguration<SQLType> externalToKnime =
                dbPort.getExternalToKnimeTypeMapping().resolve(mappingService,
                    DataTypeMappingDirection.EXTERNAL_TO_KNIME);
        final ConnectionProvider connectionProvider = session.getConnectionProvider();
        final DBConnectionController connectionController =
                session.getSessionInformation().getConnectionController();
        if (!(connectionController instanceof UserDBConnectionController)) {
            throw new InvalidSettingsException("Could not extract user name and password from input connection");
        }
        final UserDBConnectionController userController = (UserDBConnectionController)connectionController;
        if (userController.getAuthenticationType().equals(AuthenticationType.KERBEROS)) {
            throw new InvalidSettingsException("Node supports only user name and password based connections");
        }

        //use java reflection to call the two private methods
        final Class<? extends DBConnectionController> userControllerClass = connectionController.getClass();
        final Method userMethod = userControllerClass.getDeclaredMethod("getUser");
        userMethod.setAccessible(true);
        final String user = (String)userMethod.invoke(connectionController);
        final Method pwdMethod = userControllerClass.getDeclaredMethod("getPassword");
        pwdMethod.setAccessible(true);
        final String pwd = (String)pwdMethod.invoke(connectionController);
        
        final String outputQuery;
        try (Connection connection = connectionProvider.getConnection(exec)) {
            outputQuery = runRScript(exec, connection, inData, dbPort.getData().getQuery().getQuery(),
                user, pwd, (tableName) -> {
                    //we cannot use the agent here since it would wait for the already obtained session to be available
                    final SQLQuery ifExistsQuery = session.getDialect().dataManipulation().getTableExistsStatement(
                        new DefaultDBTable(tableName));
                    try (Statement statement = connection.createStatement();
                            ResultSet executeQuery = statement.executeQuery(ifExistsQuery.getQuery())) {
                        return true;
                    } catch (SQLException e) {
                        return false;
                    }
                });
        }
        final DBDataObject resultData = session.getAgent(DBMetadataReader.class).getDBDataObject(exec,
            new SQLQuery(outputQuery), externalToKnime);

        return new PortObject[] {new DBDataPortObject(dbPort, resultData)};
    }
}
