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

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;
import org.knime.r.FlowVariableRepository;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetNodeModel;
import org.knime.r.RSnippetSettings;

/**
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 */
public class MSSQLRNodeModel extends RSnippetNodeModel {

    /**
     * RSnippet Node Config for this RSnippet Node
     * @author Jonathan Hale
     */
    public static class RNodeConfig extends RSnippetNodeConfig {
        @Override
        public Collection<PortType> getInPortTypes() {
            return Arrays.asList(DatabaseConnectionPortObject.TYPE, RPortObject.TYPE);
        }

        @Override
        protected Collection<PortType> getOutPortTypes() {
            return Arrays.asList();
        }

        @Override
        protected String getScriptPrefix() {
            return "connection_string <- \"" + jdbcUrl + "\";" //
                    + "sql <- RxInSqlServer(connectionString = connection_string);" //
                    + "local <- RxLocalSeq()";
        }

        @Override
        protected String getDefaultScript() {
            return getScriptPrefix() + ";table_name <- \"NewData\"\n" //
                + "table_sql <- RxSqlServerData(sqlQuery = sprintf(\"SELECT TOP(%s) * FROM %s\", n_rows, table_name), connectionString = connection_string)\n" //
                + "table <- rxImport(table_sql)\n";
        }

        @Override
        protected String getScriptSuffix() {
            return "";
        }

        String jdbcUrl = "";
        public void setDatabaseSettings(final DatabaseConnectionSettings settings) {
            jdbcUrl = settings.getJDBCUrl();
        }
    }

    /**
     * Constructor
     */
    public MSSQLRNodeModel() {
        super(new RNodeConfig());
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger("R in MSSQL");

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);

        assert inSpecs[0] instanceof DatabaseConnectionPortObjectSpec;
        assert inSpecs[1] instanceof RPortObjectSpec;

        final DatabaseConnectionPortObjectSpec databasePort = (DatabaseConnectionPortObjectSpec)inSpecs[0];

        /* Check if we are connected to a MSSQL database, since we require support for sq_execute_external_script */
        final DatabaseConnectionSettings databaseSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());
        if (false) { // TODO
            LOGGER.error("Deploy R To MSSQL Node does not support " + databasePort.getDatabaseIdentifier());
            throw new InvalidSettingsException("This node only works with Microsoft SQL Server 2016+");
        }

        ((RNodeConfig)getRSnippetNodeConfig()).setDatabaseSettings(databaseSettings);

        return new PortObjectSpec[]{new DatabaseConnectionPortObjectSpec(databaseSettings)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final DatabaseConnectionPortObject databasePort = (DatabaseConnectionPortObject)inData[0];
        final RPortObject rPort = (RPortObject)inData[1];

        final DatabaseConnectionSettings connectionSettings =
            databasePort.getConnectionSettings(getCredentialsProvider());
        final Connection connection = connectionSettings.createConnection(getCredentialsProvider());

        LOGGER.error(connectionSettings.getJDBCUrl());

        super.execute(inData, exec);

        /* Serialize R Model */
        final RSnippetSettings settings = getRSnippet().getSettings(); // FIXME: Oh boy, how bad can it get...
        getRSnippet().getSettings().loadSettings(settings);

        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());

        return new PortObject[]{};
    }

}
