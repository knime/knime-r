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
 * History
 *   17.09.2007 (thiel): created
 */
package org.knime.r;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RCommandQueue;
import org.knime.r.controller.RController;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * The <code>RSnippetNodeModel</code> provides functionality to create a R
 * script with user defined R code and run it.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
public class RSnippetNodeModel extends ExtToolOutputNodeModel {
	private final RSnippet m_snippet;
	private final RSnippetNodeConfig m_config;
	private static final NodeLogger LOGGER = NodeLogger.getLogger("R Snippet");

	private boolean m_hasROutPorts = true;

	/**
	 * Creates new instance of <code>RSnippetNodeModel</code> with one data in
	 * and data one out port.
	 *
	 * @param pref
	 *            R preference provider
	 */
	public RSnippetNodeModel(final RSnippetNodeConfig config) {
		super(config.getInPortTypes().toArray(new PortType[config.getInPortTypes().size()]),
				config.getOutPortTypes().toArray(new PortType[config.getOutPortTypes().size()]));
		m_snippet = new RSnippet();
		m_snippet.attachLogger(LOGGER);
		m_snippet.getSettings().setScript(config.getDefaultScript());
		m_config = config;
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec tableSpec = null;
		for (final PortObjectSpec inSpec : inSpecs) {
			if (inSpec instanceof DataTableSpec) {
				tableSpec = (DataTableSpec) inSpec;
			}
		}

		final FlowVariableRepository flowVarRepository = new FlowVariableRepository(getAvailableInputFlowVariables());

		final DataTableSpec report = m_snippet.configure(tableSpec, flowVarRepository); // TODO
																						// Deadcode?

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
			final ExecutionContext exec) throws CanceledExecutionException, RException {
		m_snippet.getSettings().loadSettings(settings);

		final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());

		final RController controller = new RController();
		try {
			exec.setMessage("R is busy waiting...");
			exec.checkCanceled();
			final PortObject[] out = executeSnippet(controller, inData, flowVarRepo, exec);

			pushFlowVariables(flowVarRepo);

			return out;
		} finally {
			controller.close();
		}
	}

	/**
	 * Execute the R snippet stored in the node settings
	 *
	 * @param controller
	 *            RController to use for execution
	 *
	 * @param inData
	 *            input ports to pass to R
	 *
	 * @param flowVarRepo
	 *            flow variables to pass to R
	 *
	 * @param exec
	 *            ExecutionContext which enables cancelling of the execution.
	 */
	private PortObject[] executeSnippet(final RController controller, final PortObject[] inData,
			final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws CanceledExecutionException {
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
			exec.setMessage("Exporting data to R");
			// write all input data to the R session
			exportData(controller, inData, flowVarRepo, exec.createSubExecutionContext(0.6 - importTime));

			exec.setMessage("Running R");
			tempWorkspaceFile = FileUtil.createTempFile("R-workspace", ".RData");
			controller.saveWorkspace(tempWorkspaceFile, exec);
			runRScript(controller, tempWorkspaceFile, inData, exec.createSubExecutionContext(1.0 - importTime));
			exec.setProgress(1.0 - importTime);

			exec.setMessage("Importing data from R");
			final Collection<PortObject> outPorts = new ArrayList<PortObject>(4);
			for (final PortType portType : m_config.getOutPortTypes()) {
				if (portType.equals(BufferedDataTable.TYPE)) {
					outPorts.add(importDataFromR(controller, m_snippet.getSettings().getOutNonNumbersAsMissing(),
							exec.createSubExecutionContext(1.0)));
				} else if (portType.equals(RPortObject.TYPE)) {
					final List<String> librariesInR = importListOfLibrariesFromR(controller);
					outPorts.add(new RPortObject(tempWorkspaceFile, librariesInR));
				}
			}
			exec.setMessage("Importing flow variables from R");
			importFlowVariablesFromR(controller, flowVarRepo, exec);

			return outPorts.toArray(new PortObject[outPorts.size()]);

		} catch (final Exception e) {
			if (e instanceof CanceledExecutionException) {
				throw (CanceledExecutionException) e;
			}
			throw new RuntimeException("Execution failed.", e);
		}
	}

	private void runRScript(final RController controller, final File tempWorkspaceFile, final PortObject[] inData,
			final ExecutionContext exec) throws Exception {

		final String rScript = buildRScript(inData, tempWorkspaceFile);

		exec.setMessage("Setting up output capturing");
		// see javadoc of CAPTURE_OUTPUT_PREFIX for more information
		controller.monitoredEval(RCommandQueue.CAPTURE_OUTPUT_PREFIX, exec);

		exec.setMessage("Executing R script");
		controller.monitoredEval(rScript, exec);

		exec.setMessage("Collecting captured output");
		// see javadoc of CAPTURE_OUTPUT_PREFIX for more information
		REXP output = controller.monitoredEval(RCommandQueue.CAPTURE_OUTPUT_POSTFIX, exec);

		// process the return value of error capturing and update Error and
		// Output views accordingly
		if (output != null && output.isString()) {
			String out = output.asStrings()[0];
			if (!out.isEmpty()) {
				setExternalOutput(getLinkedListFromOutput(out));
			}

			String err = output.asStrings()[1];
			if (!err.isEmpty()) {
				setExternalErrorOutput(getLinkedListFromOutput(err));
			}
		}
	}

	private static final LinkedList<String> getLinkedListFromOutput(final String output) {
		final LinkedList<String> list = new LinkedList<>();
		Arrays.stream(output.split("\\r?\\n")).forEach((s) -> list.add(s));
		return list;
	}

	/**
	 * Deletes the specified file. If the file is a directory the directory
	 * itself as well as its files and sub-directories are deleted.
	 *
	 * @param file
	 *            The file to delete.
	 * @return <code>true</code> if the file could be deleted, otherwise
	 *         <code>false</code>.
	 */
	static boolean deleteFile(final File file) {
		boolean del = false;
		if (file != null && file.exists()) {
			del = FileUtil.deleteRecursively(file);

			// if file could not be deleted call GC and try again
			if (!del) {
				// It is possible that there are still open streams around
				// holding the file. Therefore these streams, actually belonging
				// to the garbage, has to be collected by the GC.
				System.gc();

				// try to delete again
				del = FileUtil.deleteRecursively(file);
				if (!del) {
					// ok that's it no trials anymore ...
					LOGGER.debug(file.getAbsoluteFile() + " could not be deleted !");
				}
			}
		}
		return del;
	}

	/*
	 * Create an R script which loads the input ports workspaces and sets the
	 * working directory
	 */
	private String buildRScript(final PortObject[] inPorts, final File tempWorkspaceFile) throws BadLocationException {
		final StringBuilder rScript = new StringBuilder();
		// set working directory
		rScript.append("setwd(\"");
		rScript.append(tempWorkspaceFile.getParentFile().getAbsolutePath().replace('\\', '/'));
		rScript.append("\");\n");

		// load workspaces from the input ports
		for (final PortObject port : inPorts) {
			if (port instanceof RPortObject) {
				final RPortObject rPortObject = (RPortObject) port;
				final File portFile = rPortObject.getFile();
				rScript.append("load(\"");
				rScript.append(portFile.getAbsolutePath().replace('\\', '/'));
				rScript.append("\")\n");
				rScript.append(RController.createLoadLibraryFunctionCall(rPortObject.getLibraries(), false));
			}
		}

		// add node specific prefix
		rScript.append(m_config.getScriptPrefix()).append("\n");

		// user defined script
		final String userScript = m_snippet.getDocument().getText(0, m_snippet.getDocument().getLength()).trim();
		rScript.append(RCommandQueue.makeConsoleLikeCommand(userScript) + "\n");

		// append node specific suffix
		rScript.append(m_config.getScriptSuffix()).append("\n");

		// assign list of loaded libraries so that we can read it out later
		rScript.append(RController.R_LOADED_LIBRARIES_VARIABLE + " <- (.packages());\n");

		if (m_hasROutPorts) {
			// save workspace to temporary file
			rScript.append("save.image(\"").append(tempWorkspaceFile.getAbsolutePath().replace('\\', '/'))
					.append("\");\n");
		}

		return rScript.toString();
	}

	private BufferedDataTable importDataFromR(final RController controller, final boolean nonNumbersAsMissing,
			final ExecutionContext exec) throws RException, CanceledExecutionException {
		BufferedDataTable out = controller.importBufferedDataTable("knime.out", nonNumbersAsMissing, exec);
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
	 * Export the input data into a R workspace
	 *
	 * @param inData
	 * @param flowVarRepo
	 * @param exec
	 * @return
	 * @throws REngineException
	 * @throws REXPMismatchException
	 * @throws IOException
	 * @throws CanceledExecutionException
	 */
	private void exportData(final RController controller, final PortObject[] inData,
			final FlowVariableRepository flowVarRepo, final ExecutionContext exec)
					throws RException, CanceledExecutionException {
		controller.clearWorkspace(exec);
		BufferedDataTable inTable = null;
		for (final PortObject in : inData) {
			if (in instanceof BufferedDataTable) {
				inTable = (BufferedDataTable) in;
			}
		}
		if (inTable != null) {
			controller.monitoredAssign("knime.in", inTable, exec);
		}
		controller.exportFlowVariables(flowVarRepo.getInFlowVariables(), "knime.flow.in", exec);
	}

	/**
	 * Push changed flow variables.
	 */
	private void pushFlowVariables(final FlowVariableRepository flowVarRepo) {
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

	public RSnippetSettings getSettings() {
		return m_snippet.getSettings();
	}

	protected RSnippetNodeConfig getRSnippetNodeConfig() {
		return m_config;
	}

	protected RSnippet getRSnippet() {
		return m_snippet;
	}

}
