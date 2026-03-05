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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.scripting.GenericInitialDataBuilder;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;

/**
 * WebUI scripting dialog for the R Snippet node. Serves out a custom interactive R frontend with a live console,
 * "Run Script", "Run Selection", and "Reset Workspace" capabilities.
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
class RSnippetScriptingNodeDialog extends AbstractRScriptingNodeDialog {

    /**
     * Column alias template for R: inserts column name as a data frame column access expression.
     * Dragging a column from the side panel inserts e.g. {@code knime.in[["myColumn"]]}.
     */
    private static final String COLUMN_ALIAS_TEMPLATE =
        "knime.in[[\"{{~{ subItems.[0].name }~}}\"]]";

    /**
     * Flow variable alias template for R: inserts flow variable name as a knime.flow.in access expression.
     * Dragging a flow variable from the side panel inserts e.g. {@code knime.flow.in[["myVar"]]}.
     */
    private static final String FLOWVAR_ALIAS_TEMPLATE =
        "knime.flow.in[[\"{{~{ subItems.[0].name }~}}\"]]";

    /** All flow variable types supported by the R scripting engine. */
    private static final Set<VariableType<?>> SUPPORTED_VARIABLE_TYPES = Set.of( //
        VariableType.StringType.INSTANCE, //
        VariableType.IntType.INSTANCE, //
        VariableType.DoubleType.INSTANCE //
    );

    RSnippetScriptingNodeDialog() {
        super(RSnippetNodeParameters.class, new RScriptingService());
    }

    @Override
    protected GenericInitialDataBuilder getInitialData(final NodeContext context) {
        var workflowControl = new WorkflowControl(context.getNodeContainer());
        return GenericInitialDataBuilder.createDefaultInitialDataBuilder(context) //
            .addDataSupplier("inputObjects", () -> getInputTableModel(workflowControl)) //
            .addDataSupplier("flowVariables", () -> getFlowVariablesModel(workflowControl)) //
            .addDataSupplier("outputObjects", Collections::emptyList) //
            .addDataSupplier("language", () -> "r") //
            .addDataSupplier("fileName", () -> "script.R") //
            .addDataSupplier("mainScriptConfigKey", () -> "script"); // must match @Persist(configKey = "script") in RSnippetNodeParameters
    }

    private static List<InputOutputModel> getInputTableModel(final WorkflowControl workflowControl) {
        var inputSpecs = Optional.ofNullable(workflowControl.getInputSpec()).orElse(new org.knime.core.node.port.PortObjectSpec[0]);
        if (inputSpecs.length > 0 && inputSpecs[0] instanceof DataTableSpec tableSpec) {
            var model = InputOutputModel.table() //
                .name("knime.in") //
                .codeAlias("knime.in") //
                .subItemCodeAliasTemplate(COLUMN_ALIAS_TEMPLATE) //
                .subItems(tableSpec, dt -> dt.getName()) //
                .build();
            return List.of(model);
        }
        return Collections.emptyList();
    }

    private static InputOutputModel getFlowVariablesModel(final WorkflowControl workflowControl) {
        var flowObjectStack = workflowControl.getFlowObjectStack();
        var flowVariables = Optional.ofNullable(flowObjectStack) //
            .map(stack -> stack.getAllAvailableFlowVariables().values()) //
            .orElseGet(Collections::emptyList);
        return InputOutputModel.flowVariables() //
            .subItemCodeAliasTemplate(FLOWVAR_ALIAS_TEMPLATE) //
            .subItems(flowVariables, SUPPORTED_VARIABLE_TYPES::contains) //
            .build();
    }

}
