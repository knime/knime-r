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
package org.knime.r;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.swing.event.EventListenerList;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadUtils;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.r.REvent.RListener;
import org.knime.r.rserve.RConnectionFactory;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

import com.sun.jna.Platform;

/**
 * RController
 *
 * This class manages some way of communicating with R, executing R code and
 * moving data back and forth.
 *
 * Currently, this class is a singleton and enforces mutual exclusion.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
public class RController implements AutoCloseable {

	private final NodeLogger LOGGER = NodeLogger.getLogger(RController.class);

	private static final String TEMP_VARIABLE_NAME = "knimertemp836481";

	private final RCommandQueue m_commandQueue;
	private final RConsoleController m_consoleController;
	private RConnection m_engine;

	private final EventListenerList listenerList = new EventListenerList();

	private boolean m_isRAvailable;

	private List<String> m_warnings;

	private List<String> m_errors;

	private Properties m_rProps;

	private String m_rMemoryLimit;

	private String m_rHome;

	private boolean m_wasRAvailable;

	static final String R_LOADED_LIBRARIES_VARIABLE = "knime.loaded.libraries";

	private boolean rHomeChanged() {
		final String rHome = RPreferenceInitializer.getRProvider().getRHome();
		return !m_rHome.equals(rHome);
	}

	/**
	 * Constructor
	 */
	public RController() {
		m_consoleController = new RConsoleController(this);
		m_wasRAvailable = false;
		m_isRAvailable = false;

		m_commandQueue = new RCommandQueue(this);
		m_commandQueue.addRCommandExecutionListener(m_consoleController);

		initR();
	}

	/**
	 * Create and initialize a R connection
	 *
	 * @return the new RConnection
	 * @throws Exception
	 */
	private RConnection initRConnection() throws Exception {
		final RConnection connection = RConnectionFactory.createConnection();
		if (!connection.isConnected()) {
			throw new Exception(
					"RServe could not connect. Please visit http://tech.knime.org/faq#q25 for more information."); // TODO
		}
		return connection;
	}

	/**
	 * Initialize the underlying REngine with a backend.
	 */
	private void initR() {
		try {
			m_errors = new ArrayList<String>();
			m_warnings = new ArrayList<String>();
			if (m_wasRAvailable) {
				// FIXME: Causes KNIME To crash, workaround is now to require a
				// restart.
				// m_engine.close();
				// m_consoleController.stop();
				m_isRAvailable = false;
				m_errors.add("You must restart KNIME in order for the changes in R to take effect.");
				return;
			}

			final String rHome = org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome();

			final String rHomeCheck = RBinUtil.getDefault().checkRHome(rHome);
			if (rHomeCheck != null) {
				m_errors.add(rHomeCheck);
				m_isRAvailable = false;
				return;
			}
			m_rHome = rHome;
			m_rProps = RBinUtil.getDefault().retrieveRProperties();

			if (!m_rProps.containsKey("major")) {
				m_errors.add(
						"Cannot determine major version of R. Please check the R installation defined in the KNIME preferences.");
				m_isRAvailable = false;
				return;
			}

			m_rMemoryLimit = m_rProps.get("memory.limit").toString().trim();

			m_engine = initRConnection();

			// attach a thread to the console controller to get notify when
			// commands are executed via the console
			new Thread() {
				@Override
				public void run() {
					while (true) {
						// wait for r workspace change or at most given time
						try {
							m_consoleController.waitForWorkspaceChange();
						} catch (final InterruptedException e) {
							/* nothing to do */
						}
						// notify listeners
						fireWorkspaceChange();
					}
				}
			}.start();
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			m_errors.add(e.getMessage());
			m_isRAvailable = false;
			return;
		}
		// everything is ok.
		m_isRAvailable = true;
		m_wasRAvailable = true;

		if (Platform.isWindows()) {
			try {
				// set memory to the one of the used R
				eval("memory.limit(" + m_rMemoryLimit + ");");
			} catch (REngineException | REXPMismatchException e) {
				LOGGER.error("R initialisation failed." + e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @return {@link ValueReport} containing <code>true</code> when R is
	 *         available and correctly initialized.
	 */
	public ValueReport<Boolean> isRAvailable() {
		return new ValueReport<Boolean>(m_isRAvailable, m_errors, m_warnings);
	}

	/**
	 * @return The underlying REngine (usually an {@link RConnection})
	 */
	public RConnection getREngine() {
		return m_engine;
	}

	/**
	 * This is the controller scope command queue. If the evaluation thread is
	 * running, the commands in the queue will be continuously executed.
	 * 
	 * @return
	 */
	public RCommandQueue getCommandQueue() {
		return m_commandQueue;
	}

	/**
	 * @return a console controller for this RController
	 */
	public RConsoleController getConsoleController() {
		return m_consoleController;
	}

	/**
	 * Add a listener which is notified of workspace changes.
	 * 
	 * @param l
	 *            the listener
	 */
	public void addRListener(final RListener l) {
		listenerList.add(RListener.class, l);
	}

	/**
	 * Remove a listener.
	 * 
	 * @param l
	 *            the listener
	 * @see #addRListener(RListener)
	 */
	public void removeRListener(final RListener l) {
		listenerList.remove(RListener.class, l);
	}

	/**
	 * Inform {@link RListener}s of a workspace change.
	 */
	protected void fireWorkspaceChange() {
		final REvent e = new REvent();
		for (final RListener l : listenerList.getListeners(RListener.class)) {
			l.workspaceChanged(e);
		}
	}

	/**
	 * Evaluate R code.
	 * 
	 * @param cmd
	 *            the R code
	 * @return
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */
	public REXP eval(final String cmd) throws REngineException, REXPMismatchException {
		if (getREngine() == null) {
			throw new REngineException(null, "REngine not available");
		}
		final REXP x = getREngine().parseAndEval(cmd, null, true);
		return x;
	}

	/**
	 * Evaluate R code which is expected to take a while.
	 *
	 * @param cmd
	 * @param exec
	 * @return
	 * @throws CanceledExecutionException
	 */
	public REXP monitoredEval(final String cmd, final ExecutionMonitor exec) throws CanceledExecutionException {
		return monitoredEval(cmd, exec, true);
	}

	/**
	 * Evaluate R code in a separate thread to be able to cancel it.
	 *
	 * @param cmd
	 * @param exec
	 * @param withContext
	 *            Whether the NodeContext is required.
	 * @return
	 * @throws CanceledExecutionException
	 */
	public REXP monitoredEval(final String cmd, final ExecutionMonitor exec, final boolean withContext)
			throws CanceledExecutionException {
		try {
			return new MonitoredEval(exec, withContext).run(cmd);
		} catch (REngineException | REXPMismatchException e) {
			LOGGER.error("REngine error" + e.getMessage());
			return new REXPNull();
		}
	}

	/**
	 * Assign an R variable in a separate thread to be able to cancel it.
	 * 
	 * @param symbol
	 * @param value
	 * @param exec
	 * @throws CanceledExecutionException
	 */
	public void monitoredAssign(final String symbol, final REXP value, final ExecutionMonitor exec)
			throws CanceledExecutionException {
		try {
			new MonitoredEval(exec, false).assign(symbol, value);
		} catch (REngineException | REXPMismatchException e) {
			LOGGER.error("REngine error" + e.getMessage());
		}
	}

	/**
	 * Clear the R workspace (remove all variables and imported packages).
	 * 
	 * @param exec
	 * @throws CanceledExecutionException
	 */
	public void clearWorkspace(final ExecutionMonitor exec) throws CanceledExecutionException {
		exec.setProgress(0.0, "Clearing previous workspace");
		final StringBuilder b = new StringBuilder();
		b.append("unloader <- function() {\n");
		b.append("  defaults = getOption(\"defaultPackages\")\n");
		b.append("  installed = (.packages())\n");
		b.append("  for (pkg in installed){\n");
		b.append("      if (!(as.character(pkg) %in% defaults)) {\n");
		b.append("          if(!(pkg == \"base\")){\n");
		b.append("              package_name = paste(\"package:\", as.character(pkg), sep=\"\")\n");
		b.append("              detach(package_name, character.only = TRUE)\n");
		b.append("          }\n");
		b.append("      }\n");
		b.append("  }\n");
		b.append("}\n");
		b.append("unloader();\n");
		b.append("rm(list = ls());"); // also includes the unloader function
		monitoredEval(b.toString(), exec);
		exec.setProgress(1.0, "Clearing previous workspace");
	}

	/**
	 * @param tempWorkspaceFile
	 *            the workspace file
	 * @param exec
	 *            execution monitor to report progress on
	 * @return
	 * @throws CanceledExecutionException
	 */
	List<String> clearAndReadWorkspace(final File workspaceFile, final ExecutionMonitor exec)
			throws CanceledExecutionException {
		exec.setProgress(0.0, "Clearing previous workspace");
		clearWorkspace(exec.createSubProgress(0.3));
		exec.setMessage("Loading workspace");
		monitoredEval("load(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");",
				exec.createSubProgress(0.7));
		return importListOfLibrariesAndDelete();
	}

	/**
	 * Write R variables into a R variable in the current workspace
	 * 
	 * @param inFlowVariables
	 * @param name
	 * @param exec
	 * @throws CanceledExecutionException
	 */
	public void exportFlowVariables(final Collection<FlowVariable> inFlowVariables, final String name,
			final ExecutionMonitor exec) throws CanceledExecutionException {
		final REXP[] content = new REXP[inFlowVariables.size()];
		final String[] names = new String[inFlowVariables.size()];
		int i = 0;
		for (final FlowVariable flowVar : inFlowVariables) {
			exec.checkCanceled();
			names[i] = flowVar.getName();
			if (flowVar.getType().equals(FlowVariable.Type.INTEGER)) {
				content[i] = new REXPInteger(flowVar.getIntValue());
			} else if (flowVar.getType().equals(FlowVariable.Type.DOUBLE)) {
				content[i] = new REXPDouble(flowVar.getDoubleValue());
			} else { // string
				content[i] = new REXPString(flowVar.getStringValue());
			}
			i++;
		}

		monitoredAssign(TEMP_VARIABLE_NAME, new REXPList(new RList(content, names)), exec);
		setVariableName(name, exec);
	}

	/**
	 * Get flow variables from a variable in the underlying REngine
	 *
	 * @param variableName
	 *            Name of the variable to get as flow varaibles, or an R
	 *            expression.
	 * @return
	 */
	public Collection<FlowVariable> importFlowVariables(final String variableName) {
		final List<FlowVariable> flowVars = new ArrayList<FlowVariable>();
		try {
			final REXP value = m_engine.get(variableName, null, true);

			if (value == null) {
				// A variable with this name does not exist
				return Collections.emptyList();
			}
			final RList rList = value.asList();

			for (int c = 0; c < rList.size(); c++) {
				final REXP rexp = rList.at(c);
				if (rexp.isInteger()) {
					flowVars.add(new FlowVariable((String) rList.names.get(c), rexp.asInteger()));
				} else if (rexp.isNumeric()) {
					flowVars.add(new FlowVariable((String) rList.names.get(c), rexp.asDouble()));
				} else if (rexp.isString()) {
					flowVars.add(new FlowVariable((String) rList.names.get(c), rexp.asString()));
				}
			}
		} catch (REXPMismatchException e) {
			LOGGER.error("REXPMismatchException: " + e.getMessage() + " while parsing \"" + variableName + "\"");
		} catch (REngineException e) {
			// the variable name was not found.s
		}
		return flowVars;
	}

	/**
	 * Get List of libraries imported in the current session and then delete
	 * those imports.
	 *
	 * @return The list of deleted imports
	 */
	public List<String> importListOfLibrariesAndDelete() {
		try {
			final REXP listAsREXP = eval(R_LOADED_LIBRARIES_VARIABLE);
			eval("rm(" + R_LOADED_LIBRARIES_VARIABLE + ")");
			if (!listAsREXP.isVector()) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(listAsREXP.asStrings());
			}
		} catch (REXPMismatchException | REngineException e) {
			LOGGER.error("Rengine error" + e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * Export a {@link BufferedDataTable} into a R workspace file.
	 *
	 * @param table
	 *            Table to export
	 * @param name
	 *            Name of the variable to store the data table into.
	 * @param exec
	 *            Execution monitor to track progress
	 * @throws CanceledExecutionException
	 */
	public void exportDataTable(final BufferedDataTable table, final String name, final ExecutionMonitor exec)
			throws CanceledExecutionException {

		final long size = table.size();
		final int rowCount = (int) Math.min(size, Integer.MAX_VALUE);
		if (size != rowCount) {
			LOGGER.warn("There are more than " + Integer.MAX_VALUE + " rows in the table. The table will be cut off.");
		}

		final String[] rowNames = new String[rowCount];

		final Collection<Object> columns = initializeAndFillColumns(table, rowNames, exec.createSubProgress(0.7));
		final RList content = createContent(table, columns, exec.createSubProgress(0.9));

		if (content.size() > 0) {
			final REXPString rexpRowNames = new REXPString(rowNames);
			try {
				monitoredAssign(TEMP_VARIABLE_NAME, createDataFrame(content, rexpRowNames, exec), exec);
				setVariableName(name, exec);
			} catch (final REXPMismatchException e) {
				LOGGER.error("Cannot create data frame with data from KNIME.", e);
			}
		} else {
			try {
				// create a new empty data.frame
				eval("knime.in <- data.frame()");
			} catch (REngineException | REXPMismatchException e) {
				throw new RuntimeException(e);
			}
		}
		exec.setProgress(1.0);
	}

	/**
	 * Create an array (columns) of arrays (rows) and fill the arrays with
	 * elements from table
	 *
	 * @param table
	 *            Table to get the data to fill the arrays from
	 * @param rowNames
	 *            Names for the rows
	 * @param exec
	 *            Execution monitor to check if the node was cancelled and
	 *            update the progress display
	 */
	private Collection<Object> initializeAndFillColumns(final BufferedDataTable table, final String[] rowNames,
			final ExecutionMonitor exec) throws CanceledExecutionException {
		final DataTableSpec spec = table.getDataTableSpec();

		final int numColumns = spec.getNumColumns();
		final int rowCount = table.getRowCount();

		final ArrayList<Object> columns = new ArrayList<>(numColumns);
		for (final DataColumnSpec columnSpec : spec) {
			final DataType type = columnSpec.getType();
			if (type.isCollectionType()) {
				final DataType elementType = type.getCollectionElementType();
				if (elementType.isCompatible(BooleanValue.class)) {
					columns.add(new byte[rowCount][]);
				} else if (elementType.isCompatible(IntValue.class)) {
					columns.add(new int[rowCount][]);
				} else if (elementType.isCompatible(DoubleValue.class)) {
					columns.add(new double[rowCount][]);
				} else {
					columns.add(new String[rowCount][]);
				}
			} else {
				if (type.isCompatible(BooleanValue.class)) {
					columns.add(new byte[rowCount]);
				} else if (type.isCompatible(IntValue.class)) {
					columns.add(new int[rowCount]);
				} else if (type.isCompatible(DoubleValue.class)) {
					columns.add(new double[rowCount]);
				} else {
					columns.add(new String[rowCount]);
				}
			}
		}

		// row index
		int r = 0;
		for (final DataRow row : table) {
			exec.checkCanceled();
			exec.setProgress(r / (double) rowCount);
			rowNames[r] = row.getKey().getString();

			// column index
			int c = 0;
			for (final Object column : columns) {
				final DataType type = spec.getColumnSpec(c).getType();
				final DataCell cell = row.getCell(c);
				if (type.isCollectionType()) {
					// try get value from collection cell
					if (!cell.isMissing()) {
						final CollectionDataValue collValue = (CollectionDataValue) cell;
						final DataType elementType = type.getCollectionElementType();
						if (elementType.isCompatible(BooleanValue.class)) {
							final byte[] elementValue = new byte[collValue.size()];
							int i = 0;
							for (final DataCell entry : collValue) {
								elementValue[i] = exportBooleanValue(entry);
								i++;
							}
							final byte[][] col = (byte[][]) column;
							col[r] = elementValue;

						} else if (elementType.isCompatible(IntValue.class)) {
							final int[] elementValue = new int[collValue.size()];
							int i = 0;
							for (final DataCell entry : collValue) {
								elementValue[i] = exportIntValue(entry);
								i++;
							}
							final int[][] col = (int[][]) column;
							col[r] = elementValue;
						} else if (elementType.isCompatible(DoubleValue.class)) {
							final double[] elementValue = new double[collValue.size()];
							int i = 0;
							for (final DataCell entry : collValue) {
								elementValue[i] = exportDoubleValue(entry);
								i++;
							}
							final double[][] col = (double[][]) column;
							col[r] = elementValue;
						} else {
							final String[] elementValue = new String[collValue.size()];
							int i = 0;
							for (final DataCell entry : collValue) {
								elementValue[i] = exportStringValue(entry);
								i++;
							}
							final String[][] col = (String[][]) column;
							col[r] = elementValue;
						}
					} else {
						// TODO: Is it correct to leave element value at null?
					}
				} else {
					if (type.isCompatible(BooleanValue.class)) {
						final byte[] col = (byte[]) column;
						col[r] = exportBooleanValue(cell);
					} else if (type.isCompatible(IntValue.class)) {
						final int[] col = (int[]) column;
						col[r] = exportIntValue(cell);
					} else if (type.isCompatible(DoubleValue.class)) {
						final double[] col = (double[]) column;
						col[r] = exportDoubleValue(cell);
					} else {
						final String[] col = (String[]) column;
						col[r] = exportStringValue(cell);
					}
				}
				c++;
			}
			r++;
		}
		exec.setProgress(1.0);
		return columns;
	}

	/*
	 * Create an REXPLogical for a BooleanValue
	 */
	private byte exportBooleanValue(final DataCell cell) {
		if (cell.isMissing()) {
			return REXPLogical.NA;
		}
		return ((BooleanValue) cell).getBooleanValue() ? REXPLogical.TRUE : REXPLogical.FALSE;
	}

	/*
	 * Create an int for a IntValue
	 */
	private int exportIntValue(final DataCell cell) {
		if (cell.isMissing()) {
			return REXPInteger.NA;
		}
		return ((IntValue) cell).getIntValue();
	}

	/*
	 * Create a double for a DoubleValue
	 */
	private double exportDoubleValue(final DataCell cell) {
		if (cell.isMissing()) {
			return REXPDouble.NA;
		}
		return ((DoubleValue) cell).getDoubleValue();
	}

	/*
	 * Create a String for a StringValue
	 */
	private String exportStringValue(final DataCell cell) {
		if (!cell.isMissing()) {
			return cell.toString();
		} else {
			return null;
		}
	}

	/**
	 * Create a {@link RList} from the data table and columns.
	 *
	 * @param table
	 *            Table to get the data from
	 * @param columns
	 * @param exec
	 * @return The created RList
	 * @throws CanceledExecutionException
	 */
	private RList createContent(final BufferedDataTable table, final Collection<Object> columns,
			final ExecutionMonitor exec) throws CanceledExecutionException {
		final DataTableSpec tableSpec = table.getDataTableSpec();
		final int numColumns = tableSpec.getNumColumns();
		final RList content = new RList();

		int c = 0;
		for (final Object columnsColumn : columns) {
			// get spec and type for current column
			final DataColumnSpec colSpec = tableSpec.getColumnSpec(c);
			final DataType type = colSpec.getType();

			exec.checkCanceled();
			exec.setProgress(c++ / (double) numColumns);

			if (type.isCollectionType()) {
				final DataType elementType = type.getCollectionElementType();
				final ArrayList<REXP> rList = new ArrayList<>(numColumns);
				if (elementType.isCompatible(BooleanValue.class)) {
					final byte[][] column = (byte[][]) columnsColumn;
					for (final byte[] col : column) {
						rList.add((col == null) ? null : new REXPLogical(col));
					}
				} else if (elementType.isCompatible(IntValue.class)) {
					final int[][] column = (int[][]) columnsColumn;
					for (final int[] col : column) {
						rList.add((col == null) ? null : new REXPInteger(col));
					}
				} else if (elementType.isCompatible(DoubleValue.class)) {
					final double[][] column = (double[][]) columnsColumn;
					for (final double[] col : column) {
						rList.add((col == null) ? null : new REXPDouble(col));
					}
				} else {
					final String[][] column = (String[][]) columnsColumn;
					for (final String[] col : column) {
						rList.add((col == null) ? null : createFactor(col));
					}
				}
				content.put(colSpec.getName(), new REXPGenericVector(new RList(rList)));
			} else {
				REXP ri;
				if (type.isCompatible(BooleanValue.class)) {
					final byte[] column = (byte[]) columnsColumn;
					ri = new REXPLogical(column);
				} else if (type.isCompatible(IntValue.class)) {
					final int[] column = (int[]) columnsColumn;
					ri = new REXPInteger(column);
				} else if (type.isCompatible(DoubleValue.class)) {
					final double[] column = (double[]) columnsColumn;
					ri = new REXPDouble(column);
				} else {
					final String[] column = (String[]) columnsColumn;
					ri = createFactor(column);
				}
				content.put(colSpec.getName(), ri);
			}
		}
		exec.setProgress(1.0);
		return content;
	}

	/**
	 * Creates factor variable efficiently (based on the implementation of
	 * {@link RFactor#RFactor(String[], int)}). Fixes bug 5576: New R nodes:
	 * String columns with many and/or long values take long to load into R.
	 *
	 * @param values
	 *            non null column values
	 * @return the factor
	 */
	private static REXPFactor createFactor(final String[] values) {
		final LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
		final int[] valueIndices = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			int valueIndex;
			if (values[i] == null) { // missing
				valueIndex = REXPInteger.NA;
			} else {
				Integer index = hash.get(values[i]);
				if (index == null) {
					index = hash.size() + 1;
					hash.put(values[i], index);
				}
				valueIndex = index.intValue();
			}
			valueIndices[i] = valueIndex;
		}
		final String[] levels = hash.keySet().toArray(new String[hash.size()]);
		return new REXPFactor(valueIndices, levels);
	}

	/**
	 * Create a new R <code>data.frame</code> from the given list and rownames.
	 * 
	 * @param l
	 * @param rownames
	 * @param exec
	 * @return
	 * @throws REXPMismatchException
	 */
	public static REXP createDataFrame(final RList l, final REXP rownames, final ExecutionMonitor exec)
			throws REXPMismatchException {
		if (l == null || l.size() <= 0) {
			throw new REXPMismatchException(new REXPList(l), "data frame (must have dim>0)");
		}
		if (!(l.at(0) instanceof REXPVector)) {
			throw new REXPMismatchException(new REXPList(l), "data frame (contents must be vectors)");
		}
		return new REXPGenericVector(l,
				new REXPList(new RList(new REXP[] { new REXPString("data.frame"), new REXPString(l.keys()), rownames },
						new String[] { "class", "names", "row.names" })));
	}

	/**
	 * Assign the {@link #TEMP_VARIABLE_NAME} variable to a new variable with
	 * name <code>name</code>.
	 *
	 * @param name
	 *            Name for the variable
	 * @param exec
	 *            Execution monitor
	 * @throws CanceledExecutionException
	 */
	private void setVariableName(final String name, final ExecutionMonitor exec) throws CanceledExecutionException {
		monitoredEval(name + " <- " + TEMP_VARIABLE_NAME + "; rm(" + TEMP_VARIABLE_NAME + ")", exec);
	}

	/**
	 * Import a BufferedDataTable from the R expression <code>string</code>.
	 *
	 * @param string
	 *            R expression (variable for e.g.) to retrieve a data.frame
	 *            which is then converted into a BufferedDataTable.
	 * @param nonNumbersAsMissing
	 *            Convert NaN and Infinity to {@link MissingCell}.
	 * @param exec
	 *            Execution context for creating the table and monitoring
	 *            execution.
	 * @return The created BufferedDataTable.
	 * @throws REngineException
	 * @throws REXPMismatchException
	 * @throws CanceledExecutionException
	 */
	public BufferedDataTable importBufferedDataTable(final String string, final boolean nonNumbersAsMissing,
			final ExecutionContext exec) throws REngineException, REXPMismatchException, CanceledExecutionException {
		final REXP typeRexp = eval("class(" + string + ")");
		if (typeRexp.isNull()) {
			// a variable with this name does not exist
			final BufferedDataContainer cont = exec.createDataContainer(new DataTableSpec());
			cont.close();
			return cont.getTable();
		}

		final String type = typeRexp.asString();
		if (!type.equals("data.frame")) {
			throw new RuntimeException("Supporting 'data.frame' as return type, only.");
		}

		final String[] rowIds = eval("attr(" + string + " , \"row.names\")").asStrings();
		// TODO: Support int[] as row names or int which defines the column of
		// row names:
		// http://stat.ethz.ch/R-manual/R-patched/library/base/html/row.names.html
		final int numRows = rowIds.length;
		final int ommitColumn = -1;

		final REXP value = m_engine.get(string, null, true);
		final RList rList = value.asList();

		final DataTableSpec outSpec = createSpecFromDataFrame(rList);
		final BufferedDataContainer cont = exec.createDataContainer(outSpec);

		final int numCells = ommitColumn < 0 ? rList.size() : rList.size() - 1;
		/* now, instead try a row by row approach */
		final ArrayList<DataCell>[] columns = new ArrayList[numCells];

		int cellIndex = 0; // column index without the omitted column
		for (int columnIndex = 0; columnIndex < numCells; columnIndex++) {
			exec.checkCanceled();
			exec.setProgress(cellIndex / (double) numCells);

			if (columnIndex == ommitColumn) {
				continue;
			}

			final REXP column = rList.at(columnIndex);
			columns[cellIndex] = new ArrayList<DataCell>(numRows);

			if (column.isNull()) {
				Collections.fill(columns[columnIndex], DataType.getMissingCell());
			} else {
				if (column.isList()) {
					final ArrayList<DataCell> list = columns[columnIndex];
					for (final Object o : column.asList()) {
						final REXP rexp = (REXP) o;
						if (rexp.isNull()) {
							list.add(DataType.getMissingCell());
						} else {
							if (rexp.isVector()) {
								final REXPVector colValue = (REXPVector) rexp;
								final ArrayList<DataCell> listCells = new ArrayList<>(colValue.length());
								importCells(colValue, listCells, nonNumbersAsMissing);
								list.add(CollectionCellFactory.createListCell(listCells));
							} else {
								LOGGER.warn("Expected Vector type for list cell. Inserting missing cell instead.");
								list.add(DataType.getMissingCell());
							}
						}
					}
				} else {
					importCells(column, columns[columnIndex], nonNumbersAsMissing);
				}
			}
			cellIndex++;
		}

		final Iterator<DataCell>[] itors = new Iterator[numCells];
		for (int i = 0; i < numCells; i++) {
			itors[i] = columns[i].iterator();
		}

		final DataCell[] curRow = new DataCell[numCells];
		for (final String rowId : rowIds) {
			int i = 0;
			for (final Iterator<DataCell> itor : itors) {
				curRow[i++] = itor.next();
			}
			cont.addRowToTable(new DefaultRow(rowId, curRow));
		}

		cont.close();

		return cont.getTable();

	}

	/**
	 * Import all cells from a R expression and put them into
	 * <code>column</code>.
	 *
	 * @param rexp
	 *            Source of values
	 * @param column
	 *            ArrayList to store the created DataCells into.
	 * @param nonNumbersAsMissing
	 *            Convert NaN and Infinity to {@link MissingCell}.
	 * @throws REXPMismatchException
	 */
	private final void importCells(final REXP rexp, final ArrayList<DataCell> column, final boolean nonNumbersAsMissing)
			throws REXPMismatchException {
		if (rexp.isLogical()) {
			for (final byte val : rexp.asBytes()) {
				if (val == REXPLogical.TRUE) {
					column.add(BooleanCell.TRUE);
				} else if (val == REXPLogical.FALSE) {
					column.add(BooleanCell.FALSE);
				} else {
					column.add(DataType.getMissingCell());
				}
			}
		} else if (rexp.isFactor()) {
			for (int r = 0; r < rexp.length(); r++) {
				final String colValue = rexp.asFactor().at(r);
				column.add((colValue == null) ? DataType.getMissingCell() : new StringCell(colValue));
			}
		} else if (rexp.isInteger()) {
			for (final int val : rexp.asIntegers()) {
				column.add((val == REXPInteger.NA) ? DataType.getMissingCell() : new IntCell(val));
			}
		} else if (rexp.isNumeric()) {
			for (final double val : rexp.asDoubles()) {
				if (!REXPDouble.isNA(val) && !(nonNumbersAsMissing && (Double.isNaN(val) || Double.isInfinite(val)))) {
					/*
					 * If R value is not NA (not available), missing cell will
					 * be exported instead. Also, if nonNumbers should be
					 * exported as missing cells, NaN and Infinite will be
					 * exported as missing cells instead, aswell.
					 */
					column.add(new DoubleCell(val));
				} else {
					column.add(DataType.getMissingCell());
				}
			}
		} else {
			for (final String val : rexp.asStrings()) {
				column.add((val == null) ? DataType.getMissingCell() : new StringCell(val));
			}
		}
	}

	private DataTableSpec createSpecFromDataFrame(final RList rList) throws REXPMismatchException {
		final List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
		for (int c = 0; c < rList.size(); c++) {
			final String colName = rList.isNamed() ? rList.keyAt(c) : "R_out_" + c;
			DataType colType = null;
			final REXP column = rList.at(c);
			if (column.isNull()) {
				colType = StringCell.TYPE;
			}
			if (column.isList()) {
				colType = DataType.getType(ListCell.class, DataType.getType(DataCell.class));
			} else {
				colType = importDataType(column);
			}

			colSpecs.add(new DataColumnSpecCreator(colName, colType).createSpec());
		}
		return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
	}

	/*
	 * Get cell type as which a REXP would be imported.
	 */
	private DataType importDataType(final REXP column) {
		if (column.isNull()) {
			return StringCell.TYPE;
		} else if (column.isLogical()) {
			return BooleanCell.TYPE;
		} else if (column.isFactor()) {
			return StringCell.TYPE;
		} else if (column.isInteger()) {
			return IntCell.TYPE;
		} else if (column.isNumeric()) {
			return DoubleCell.TYPE;
		} else {
			return StringCell.TYPE;
		}
	}

	/**
	 * Save the current R session to a workspace file
	 * 
	 * @param workspaceFile
	 * @param exec
	 * @throws CanceledExecutionException
	 */
	public void saveWorkspace(final File workspaceFile, final ExecutionMonitor exec) throws CanceledExecutionException {
		// save workspace to file
		monitoredEval("save.image(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");", exec);
	}

	/**
	 * Load a list of R libraries: <code>library(libname)</code>.
	 * 
	 * @param listOfLibraries
	 * @throws REngineException
	 * @throws REXPMismatchException
	 */
	public void loadLibraries(final List<String> listOfLibraries) throws REngineException, REXPMismatchException {
		final String cmd = createLoadLibraryFunctionCall(listOfLibraries, true);
		eval(cmd);
	}

	/**
	 * A function call that loads all libraries in the argument but checking if
	 * they are not loaded yet.
	 *
	 * @param listOfLibraries
	 *            List of libraries from upstream node (e.g. randomForest, tree,
	 *            ...)
	 * @param suppressMessages
	 *            if true the library call is wrapped so that no output is
	 *            printed
	 * @return The command string to be run in R (ends with newline)
	 */
	public static String createLoadLibraryFunctionCall(final List<String> listOfLibraries, final boolean suppressMessages) {
		final StringBuilder functionBuilder = new StringBuilder();
		functionBuilder.append("function(packages_to_install) {\n");
		functionBuilder.append("  for (pkg in packages_to_install) {\n");
		functionBuilder.append("      if(!(pkg %in% (.packages()))) {\n");
		if (suppressMessages) {
			functionBuilder.append("          suppressMessages(library(pkg, character.only = TRUE))\n");
		} else {
			functionBuilder.append("          library(pkg, character.only = TRUE)\n");
		}
		functionBuilder.append("      }\n");
		functionBuilder.append("  }\n");
		functionBuilder.append("}\n");
		final StringBuilder packageVector = new StringBuilder("c(");
		for (int i = 0; i < listOfLibraries.size(); i++) {
			packageVector.append(i == 0 ? "\"" : ", \"").append(listOfLibraries.get(i)).append("\"");
		}
		packageVector.append(")");
		return "sapply(" + packageVector + ", " + functionBuilder.toString() + ")\n";
	}

	/**
	 * Evaluation of R code with a monitor in a separate thread to cancel the
	 * code execution in case the execution of the node is cancelled.
	 */
	private final class MonitoredEval {

		private final int m_interval = 200;
		private final ExecutionMonitor m_exec;
		private final boolean m_withContext;

		public MonitoredEval(final ExecutionMonitor exec, final boolean withContext) {
			m_exec = exec;
			m_withContext = withContext;
		}

		/*
		 * Run the Callable in a thread and make sure to cancel it, in case
		 * execution is cancelled.
		 */
		private REXP monitor(final Callable<REXP> task) {
			final FutureTask<REXP> runningTask = new FutureTask<>(task);
			final Thread t = (m_withContext) ? ThreadUtils.threadWithContext(runningTask, "R-Evaluation")
					: new Thread(runningTask, "R-Evaluation");
			t.start();

			try {
				while (!runningTask.isDone()) {
					Thread.sleep(m_interval);
					m_exec.checkCanceled();
				}

				return runningTask.get();
			} catch (InterruptedException | CanceledExecutionException | ExecutionException e) {
				try {
					if (!runningTask.isDone()) {
						t.interrupt();

						// The eval() call blocks somewhere in RTalk class,
						// where it waits for a socket. If we close that, we
						// should be able to force the interruption of our
						// evaluation thread.
						terminateAndRelaunch();
						// FIXME: Causes a "Socket closed" stack trace to be
						// printed. Should be cought instead, but needs to be
						// fixed in REngine first see
						// https://github.com/s-u/REngine/issues/6
					}
				} catch (final Exception e1) {
					LOGGER.warn("Could not terminate R correctly.");
				}
			}

			return null;
		}

		/**
		 * Run R code
		 *
		 * @param cmd
		 * @return Result of the code
		 * @throws REngineException
		 * @throws REXPMismatchException
		 * @throws CanceledExecutionException
		 */
		public REXP run(final String cmd) throws REngineException, REXPMismatchException, CanceledExecutionException {
			final Future<REXP> future = startMonitoredThread(() -> {
				return eval(cmd);
			});

			try {
				return future.get();
			} catch (InterruptedException | ExecutionException e) {
				return null;
			}
		}

		/**
		 * Monitored assignment of <code>value</code> to <code>symbol</code>.
		 *
		 * @param symbol
		 * @param value
		 * @throws REngineException
		 * @throws REXPMismatchException
		 * @throws CanceledExecutionException
		 */
		public void assign(final String symbol, final REXP value)
				throws REngineException, REXPMismatchException, CanceledExecutionException {
			final Future<REXP> future = startMonitoredThread(() -> {
				m_engine.assign(symbol, value);
				return null;
			});

			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
			}
		}

		/*
		 * Execute a Callable in a monitored thread
		 */
		private Future<REXP> startMonitoredThread(final Callable<REXP> task) {
			final FutureTask<REXP> ret = new FutureTask<REXP>(() -> {
				return monitor(task);
			});

			if (m_withContext) {
				ThreadUtils.threadWithContext(ret, "R-Monitor").start();
			} else {
				new Thread(ret, "R-Monitor").start();
			}
			return ret;
		}

	}

	@Override
	public void close() {
		if (m_engine != null) {
			m_engine.close();
		}
	}

	/**
	 * Terminate and relaunch the R process this controller is connected to.
	 * This is currently the only way to interrupt command execution.
	 *
	 * @throws Exception
	 */
	public void terminateAndRelaunch() throws Exception {
		LOGGER.debug("Terminating R process");

		RConnectionFactory.terminateProcessOf((RConnection) m_engine);
		m_engine.close();
		m_engine = initRConnection();
	}

	/**
	 * Terminate the R process started for this RController
	 */
	public void terminateRProcess() {
		RConnectionFactory.terminateProcessOf((RConnection) m_engine);
	}

}