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
package org.knime.r;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.RpcDataService.RpcDataServiceBuilder;
import org.knime.core.webui.node.dialog.scripting.AbstractDefaultScriptingNodeDialog;
import org.knime.core.webui.page.Page;
import org.knime.node.parameters.NodeParameters;

/**
 * Abstract base class for interactive R scripting node dialogs that serve a custom Vue frontend with console support.
 * <p>
 * Subclasses of {@link AbstractDefaultScriptingNodeDialog} but override {@link #getPage()} to serve the R-specific
 * frontend compiled into {@code js-src/dist/} of the {@code org.knime.r} bundle, and override
 * {@link #getDataServiceBuilder(NodeContext)} to register the live {@link RScriptingService}.
 * <p>
 * Concrete subclasses must implement {@link #getInitialData(NodeContext)} to provide node-specific initial data (input
 * objects, flow variables, and metadata) to the scripting editor frontend.
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
abstract class AbstractRScriptingNodeDialog extends AbstractDefaultScriptingNodeDialog {

    private final RScriptingService m_scriptingService;

    /**
     * Creates a new dialog for the given model settings class.
     *
     * @param modelSettings the {@link NodeParameters} class describing the node's settings
     * @param scriptingService the live R scripting service for this dialog instance
     */
    protected AbstractRScriptingNodeDialog(final Class<? extends NodeParameters> modelSettings,
        final RScriptingService scriptingService) {
        super(modelSettings);
        m_scriptingService = scriptingService;
    }

    /**
     * Serves the custom R scripting frontend compiled into {@code js-src/dist/} of this bundle, replacing the
     * default static scripting editor served by {@link AbstractDefaultScriptingNodeDialog}.
     */
    @Override
    public Page getPage() {
        return Page.create() //
            .fromFile() //
            .bundleClass(AbstractRScriptingNodeDialog.class) //
            .basePath("js-src/dist") //
            .relativeFilePath("index.html") //
            .addResourceDirectory("assets") //
            .addResourceDirectory("monacoeditorwork");
    }

    /**
     * Registers the live {@link RScriptingService} as the {@code "ScriptingService"} RPC endpoint, replacing the
     * no-op service used by {@link AbstractDefaultScriptingNodeDialog}.
     */
    @Override
    protected RpcDataServiceBuilder getDataServiceBuilder(final NodeContext context) {
        return RpcDataService.builder() //
            .addService("ScriptingService", m_scriptingService.getJsonRpcService()) //
            .onDeactivate(m_scriptingService::onDeactivate);
    }
}
