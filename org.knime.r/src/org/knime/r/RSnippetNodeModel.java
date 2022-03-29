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
 * History
 *   17.09.2007 (thiel): created
 */
package org.knime.r;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.text.BadLocationException;

import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.bin.preferences.DefaultRPreferenceProvider;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;
import org.knime.r.controller.ConsoleLikeRExecutor;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;

/**
 * The <code>RSnippetNodeModel</code> provides functionality to create a R script with user defined R code and run it.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
public class RSnippetNodeModel extends ExtToolOutputNodeModel {
    private static final String CONDA_EVN_TYPE_CLASS_NAME =
        "org.knime.conda.CondaEnvironmentPropagation.CondaEnvironmentType";

    private final RSnippet m_snippet;

    private final RSnippetNodeConfig m_config;

    private boolean m_hasROutPorts = true;

    private List<String> m_librariesInR = null;

    static Map<String, FlowVariable>
        getCondaVariables(final Function<VariableType<?>[], Map<String, FlowVariable>> variableGetter) {
        return variableGetter.apply(VariableTypeRegistry.getInstance().getAllTypes()).entrySet().stream()
            .filter(e -> e.getValue().getVariableType().getClass().getCanonicalName().equals(CONDA_EVN_TYPE_CLASS_NAME))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Creates new instance of <code>RSnippetNodeModel</code> with one data in and data one out port.
     *
     * @param config R Snippet Node Config
     */
    public RSnippetNodeModel(final RSnippetNodeConfig config) {
        super(config.getInPortTypes().toArray(new PortType[config.getInPortTypes().size()]),
            config.getOutPortTypes().toArray(new PortType[config.getOutPortTypes().size()]));
        m_snippet = new RSnippet();
        m_snippet.attachLogger(getLogger());
        m_snippet.getSettings().setScript(config.getDefaultScript());
        m_config = config;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final FlowVariableRepository flowVarRepository = new FlowVariableRepository(getAvailableInputFlowVariables());

        for (final FlowVariable flowVar : flowVarRepository.getModified()) {
            if (flowVar.getType().equals(Type.INTEGER)) {
                pushFlowVariableInt(flowVar.getName(), flowVar.getIntValue());
            } else if (flowVar.getType().equals(Type.DOUBLE)) {
                pushFlowVariableDouble(flowVar.getName(), flowVar.getDoubleValue());
            } else {
                pushFlowVariableString(flowVar.getName(), flowVar.getStringValue());
            }
        }

        m_hasROutPorts = false;
        final Collection<PortObjectSpec> outSpec = new ArrayList<PortObjectSpec>(4);
        for (final PortType portType : m_config.getOutPortTypes()) {
            if (portType.equals(BufferedDataTable.TYPE)) {
                outSpec.add(null);
            } else if (portType.equals(RPortObject.TYPE)) {
                outSpec.add(RPortObjectSpec.INSTANCE);
                m_hasROutPorts = true;
            }
        }
        return outSpec.toArray(new PortObjectSpec[outSpec.size()]);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        return executeInternal(m_snippet.getSettings(), inData, exec);
    }

    private PortObject[] executeInternal(final RSnippetSettings settings, final PortObject[] inData,
        final ExecutionContext exec) throws Exception {
        m_snippet.getSettings().loadSettings(settings);

        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());

        try (final RController controller = new RController(true, getRPreferences())) {
            controller.initialize();
            exec.checkCanceled();
            final PortObject[] out = executeSnippet(controller, inData, flowVarRepo, exec);

            pushFlowVariables(flowVarRepo);

            return out;
        }
    }

    /**
     * Get the R preference provider
     * @throws InvalidSettingsException if the provider selected through a conda environment variable no longer exists
     */
    private RPreferenceProvider getRPreferences() throws InvalidSettingsException {
        final RSnippetSettings snippetSettings = m_snippet.getSettings();
        if (snippetSettings.isOverwriteRHome()) {
            if (snippetSettings.hasRHomePath()) {
                return new DefaultRPreferenceProvider(snippetSettings.getRHomePath());
            } else {
                final var condaVar =
                    getCondaVariables(this::getAvailableFlowVariables).get(snippetSettings.getRCondaVariableName());
                if (condaVar != null) {
                    return new DefaultRPreferenceProvider(condaVar);
                } else {
                    throw new InvalidSettingsException("Please select a valid conda environment flow variable");
                }
            }
        } else {
            return RPreferenceInitializer.getRProvider();
        }
    }

    /**
     * Execute the R snippet stored in the node settings
     *
     * @param controller RController to use for execution
     * @param inData input ports to pass to R
     * @param flowVarRepo flow variables to pass to R
     * @param exec ExecutionContext which enables canceling of the execution.
     * @throws Exception
     */
    protected PortObject[] executeSnippet(final RController controller, final PortObject[] inData,
        final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws Exception {
        return executeSnippet(controller, getScript(), inData, flowVarRepo, exec);
    }

    /**
     * Execute the R snippet stored in the node settings
     *
     * @param controller RController to use for execution
     * @param script Script to run
     * @param inData input ports to pass to R
     * @param flowVarRepo flow variables to pass to R
     * @param exec ExecutionContext which enables canceling of the execution.
     * @throws Exception
     */
    protected PortObject[] executeSnippet(final RController controller, final String script, final PortObject[] inData,
        final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws Exception {
        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<String>());
        setFailedExternalOutput(new LinkedList<String>());

        File tempWorkspaceFile = null;
        try {
            // just to have a better progress report
            double importTime = 0.0;
            for (final PortType portType : m_config.getOutPortTypes()) {
                if (portType.equals(BufferedDataTable.TYPE)) {
                    importTime = 0.3;
                }
            }
            exec.setProgress(0.0);

            final RSnippetSettings s = m_snippet.getSettings();
            controller.importDataFromPorts(inData, exec.createSubExecutionContext(importTime), s.getSendBatchSize(),
                s.getKnimeInType(), s.getSendRowNames());
            controller.exportFlowVariables(flowVarRepo.getInFlowVariables(), "knime.flow.in", exec);

            tempWorkspaceFile = FileUtil.createTempFile("R-workspace", ".RData");
            runRScript(controller, script, tempWorkspaceFile, inData, exec.createSubExecutionContext(1.0 - importTime));
            exec.setProgress(1.0 - importTime);

            exec.setMessage("Importing data from R");
            final Collection<PortObject> outPorts = new ArrayList<PortObject>(4);
            for (final PortType portType : m_config.getOutPortTypes()) {
                if (portType.equals(BufferedDataTable.TYPE)) {
                    outPorts.add(importDataFromR(controller, m_snippet.getSettings().getOutNonNumbersAsMissing(),
                        exec.createSubExecutionContext(0.3)));
                } else if (portType.equals(RPortObject.TYPE)) {
                    outPorts.add(new RPortObject(tempWorkspaceFile, m_librariesInR));
                }
            }
            exec.setMessage("Importing flow variables from R");
            importFlowVariablesFromR(controller, flowVarRepo, exec);

            return outPorts.toArray(new PortObject[outPorts.size()]);

        } catch (final Exception e) {
            if (e instanceof CanceledExecutionException) {
                throw (CanceledExecutionException)e;
            }
            throw e;
        }
    }

    /**
     * @return Script to be run in
     *         {@link #executeSnippet(RController, PortObject[], FlowVariableRepository, ExecutionContext)}
     */
    protected String getScript() {
        try {
            return m_snippet.getDocument().getText(0, m_snippet.getDocument().getLength()).trim();
        } catch (final BadLocationException e) {
            // Will never happen
            throw new IllegalStateException(e);
        }
    }

    private void runRScript(final RController controller, final String script, final File tempWorkspaceFile,
        final PortObject[] inData, final ExecutionContext exec) throws Exception {

        final ConsoleLikeRExecutor executor = new ConsoleLikeRExecutor(controller);

        exec.setMessage("Setting up output capturing");
        executor.setupOutputCapturing(exec);

        exec.setMessage("Executing R script");

        // run prefix and script itself
        executor.executeIgnoreResult("setwd(\"" + tempWorkspaceFile.getParentFile().getAbsolutePath().replace('\\', '/')
            + "\")\n" + m_config.getScriptPrefix() + "\n" + script, exec);
        // run postfix in a separate evaluation to make sure we are not preventing the return value of the script being printed, which is
        // important for ggplot2 graphs, which would otherwise not be drawn onto the graphics (png) device.
        executor.executeIgnoreResult(
            m_config.getScriptSuffix() + "\n" + RController.R_LOADED_LIBRARIES_VARIABLE + "<-(.packages())", exec);

        exec.setMessage("Collecting captured output");
        executor.finishOutputCapturing(exec);

        // process the return value of error capturing and update Error and
        // Output views accordingly
        if (!executor.getStdOut().isEmpty()) {
            setExternalOutput(getLinkedListFromOutput(executor.getStdOut()));
        }

        if (!executor.getStdErr().isEmpty()) {
            final LinkedList<String> output = getLinkedListFromOutput(executor.getStdErr());
            setExternalErrorOutput(output);
            /* Only fail node if one of the lines starts with "Error:", as some functions (like require() ) output
             * to stderr on success. */
            boolean isError =
                output.stream().filter(s -> s.startsWith(ConsoleLikeRExecutor.ERROR_PREFIX)).findAny().isPresent();
            if (isError) {
                throw new RException("Error in R code: \n" + String.join("\n", output), null);
            }
        }

        // cleanup temporary variables of output capturing and
        // consoleLikeCommand stuff
        exec.setMessage("Cleaning up");
        executor.cleanup(exec);

        if (m_hasROutPorts) {
            // save workspace to temporary file
            m_librariesInR = importListOfLibrariesFromR(controller);
            controller.saveWorkspace(tempWorkspaceFile, exec);
        }

    }

    private static final LinkedList<String> getLinkedListFromOutput(final String output) {
        final LinkedList<String> list = new LinkedList<>();
        Arrays.stream(output.split("\\r?\\n")).forEach((s) -> list.add(s));
        return list;
    }

    @Override
    protected void reset() {
        super.reset();

        m_librariesInR = null;
    }

    private BufferedDataTable importDataFromR(final RController controller, final boolean nonNumbersAsMissing,
        final ExecutionContext exec) throws RException, CanceledExecutionException {
        final BufferedDataTable out = controller.importBufferedDataTable("knime.out", nonNumbersAsMissing, exec);
        return out;
    }

    private void importFlowVariablesFromR(final RController controller, final FlowVariableRepository flowVarRepo,
        final ExecutionContext exec) throws RException, CanceledExecutionException {
        final Collection<FlowVariable> flowVars = controller.importFlowVariables("knime.flow.out");
        for (final FlowVariable flowVar : flowVars) {
            flowVarRepo.put(flowVar);
        }
    }

    private List<String> importListOfLibrariesFromR(final RController controller) throws RException {
        return controller.importListOfLibrariesAndDelete();
    }

    /**
     * Push changed flow variables.
     */
    protected void pushFlowVariables(final FlowVariableRepository flowVarRepo) {
        for (final FlowVariable var : flowVarRepo.getModified()) {
            final Type type = var.getType();
            if (type.equals(Type.INTEGER)) {
                pushFlowVariableInt(var.getName(), var.getIntValue());
            } else if (type.equals(Type.DOUBLE)) {
                pushFlowVariableDouble(var.getName(), var.getDoubleValue());
            } else { // case: type.equals(Type.STRING)
                pushFlowVariableString(var.getName(), var.getStringValue());
            }
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_snippet.getSettings().saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final RSnippetSettings s = new RSnippetSettings();
        s.loadSettings(settings);
        // TODO: Check settings
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_snippet.getSettings().loadSettings(settings);
    }

    /**
     * Get the settings for the R snippet
     *
     * @return the settings
     *
     * @see RSnippet#getSettings()
     */
    public RSnippetSettings getSettings() {
        return m_snippet.getSettings();
    }

    /**
     * Get the R snippet node config
     *
     * @return the config
     */
    protected RSnippetNodeConfig getRSnippetNodeConfig() {
        return m_config;
    }

    /**
     * Get the underlying {@link RSnippet}.
     *
     * @return the RSnippet
     */
    protected RSnippet getRSnippet() {
        return m_snippet;
    }
}
