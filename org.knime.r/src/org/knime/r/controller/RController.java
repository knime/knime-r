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
package org.knime.r.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.LinkedBlockingQueue;

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
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadUtils;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.RBinUtil.InvalidRHomeException;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.r.rserve.RConnectionFactory;
import org.knime.r.rserve.RConnectionFactory.RConnectionResource;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

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
public class RController implements IRController {

	private final NodeLogger LOGGER = NodeLogger.getLogger(getClass());

	private static final String TEMP_VARIABLE_NAME = "knimertemp836481";
	public static final String R_LOADED_LIBRARIES_VARIABLE = "knime.loaded.libraries";

	private RConnectionResource m_connection;

	private Properties m_rProps;

	private boolean m_initialized = false;
	private boolean m_useNodeContext = false;

	/**
	 * Constructor. Calls {@link #initialize()}. To avoid initialization, use
	 * {@link #RController(boolean)}.
	 *
	 * @throws RException
	 */
	public RController() throws RException {
		this(false);
		initialize();
	}

	/**
	 * Constructor
	 *
	 * @param useNodeContext
	 *            Whether to use the NodeContext for threads
	 * @throws RException
	 */
	public RController(final boolean useNodeContext) {
		setUseNodeContext(useNodeContext);
	}

	// --- NodeContext handling ---

	@Override
	public void setUseNodeContext(final boolean useNodeContext) {
		m_useNodeContext = useNodeContext;
	}

	@Override
	public boolean isUsingNodeContext() {
		return m_useNodeContext;
	}

	// --- Initialization & RConnection lifecycle ---

	@Override
	public void initialize() throws RException {
		initR();
	}

	/**
	 * Check if the RController is initialized and throws
	 * {@link RControllerNotInitializedException} if not.
	 */
	private final void checkInitialized() {
		if (!m_initialized || m_connection == null) {
			throw new RControllerNotInitializedException();
		}
		if (!m_connection.isRInstanceAlive()) {
			throw new RuntimeException("Rserve process terminated unexpectedly.");
		}
		if (m_connection.isAvailable()) {
			// resource should never be available, if held by this RController.
			// Available means available to aqcuire for other RControllers.
			throw new RuntimeException("Invalid resource state: lost ownership of connection resource.");
		}
	}

	@Override
	public void close() throws RException {
		if (m_connection != null) {
			m_connection.release();
			m_connection = null;
		}

		m_initialized = false;
	}

	/**
	 * Terminate and relaunch the R process this controller is connected to.
	 * This is currently the only way to interrupt command execution.
	 *
	 * @throws Exception
	 */
	public void terminateAndRelaunch() throws Exception {
		LOGGER.debug("Terminating R process");

		terminateRProcess();

		try {
			m_connection = initRConnection();
			m_initialized = (m_connection != null && m_connection.get().isConnected());
		} catch (Exception e) {
			throw new Exception("Initializing R with Rserve failed.", e);
		}
	}

	/**
	 * Terminate the R process started for this RController
	 */
	public void terminateRProcess() {
		if (m_connection != null) {
			m_connection.destroy(true);
		}

		m_initialized = false;
	}

	/**
	 * Check if the connection is still valid and recover if not.
	 *
	 * @throws IOException
	 */
	public void checkConnectionAndRecover() throws Exception {
		if (m_connection != null && m_connection.get().isConnected() && m_connection.isRInstanceAlive()) {
			// connection is fine.
			return;
		}

		// all of the session data has been lost. We cannot recover from that.
		terminateAndRelaunch();
	}

	/**
	 * Create and initialize a R connection
	 *
	 * @return the new RConnection
	 * @throws Exception
	 */
	private RConnectionResource initRConnection() throws RserveException, IOException {
		final RConnectionResource resource = RConnectionFactory.createConnection();

		if (!resource.get().isConnected()) {
			throw new IOException("Could not initialize RController: Resource was not connected.");
		}
		return resource;
	}

	/**
	 * Initialize the underlying REngine with a backend.
	 *
	 * @throws RException
	 */
	private void initR() throws RException {
		try {
			final String rHome = org.knime.ext.r.bin.preferences.RPreferenceInitializer.getRProvider().getRHome();
			RBinUtil.checkRHome(rHome);

			m_rProps = RBinUtil.retrieveRProperties();

			if (!m_rProps.containsKey("major")) {
				throw new RException(
						"Cannot determine major version of R. Please check the R installation defined in the KNIME preferences.");
			}

			final String rserveProp = m_rProps.getProperty("Rserve.path");
			if (rserveProp == null || rserveProp.isEmpty()) {
				org.knime.ext.r.bin.preferences.RPreferenceInitializer.invalidatePreferenceProviderCache();
				throw new RException(
						"Could not find Rserve package. Please install it in your R installation by running \"install.packages('Rserve')\".");
			}
			m_connection = initRConnection();
		} catch (final InvalidRHomeException ex) {
			throw new RException("R Home is invalid.", ex);
		} catch (final RserveException | IOException e) {
			throw new RException("Exception occured during R initialization.", e);
		}

		m_initialized = (m_connection != null && m_connection.get().isConnected());

		if (Platform.isWindows()) {
			try {
				final String rMemoryLimit = m_rProps.get("memory.limit").toString().trim();
				// set memory to the one of the used R
				eval("memory.limit(" + rMemoryLimit + ");");
			} catch (Exception e) {
				LOGGER.error("R initialisation failed. " + e.getMessage());
				throw new RuntimeException(e);
			}
		} else if (Platform.isMac()) {
			// produce a warning message if 'Cairo' package is not installed.
			try {
				final REXP ret = eval("find.package('Cairo')");
				final String cairoPath = ret.asString();

				if (cairoPath == null || cairoPath.isEmpty()) {
					// under Mac we need Cairo package to use png()/bmp() etc devices.
					throw new RException("");
				}

			} catch (RException | REXPMismatchException e) {
				LOGGER.warn("The package 'Cairo' needs to be installed in your R installation for bitmap graphics "
						+ "devices to work properly. Please install it in R using \"install.packages('Cairo')\".");
				return;
			}

			// Cairo requires XQuartz to be installed. We make sure it is, since
			// loading the Cairo library will crash Rserve otherwise.
			final ProcessBuilder builder = new ProcessBuilder("mdls", "-name", "kMDItemVersion",
					"/Applications/Utilities/XQuartz.app");

			boolean found = false;
			try {
				final Process process = builder.start();

				// check if output of process was a valid version
				final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = stdout.readLine()) != null) {
					if (line.matches("kMDItemVersion = \"2(?:\\.[0-9]+)*\"")) {
						found = true;
					}
				}

				try {
					process.waitFor();
				} catch (InterruptedException e) {
					// happens when user cancels node at this point for example
					LOGGER.debug("Interrupted while waiting for mdls process to terminate.", e);
				}

			} catch (IOException e) {
				// should never happen, just in case, here is something for
				// users to report if they accidentally deleted their mdls
				LOGGER.error("Could not run mdls to check for XQuartz version.");
			}

			if (!found) {
				throw new RuntimeException("XQuartz is required for the Cairo library on MacOS. Please download "
						+ "and install XQuartz from www.xquartz.org.");
			}
		}
	}

	// --- Simple Getters ---

	@Override
	public RConnection getREngine() {
		checkInitialized();

		return m_connection.get();
	}

	@Override
	public boolean isInitialized() {
		return m_initialized;
	}

	// --- R evaluation ---

	@Override
	public REXP eval(final String expr) throws RException {
		try {
			synchronized (getREngine()) {
				REXP x = getREngine().parseAndEval(expr, null, true);
				return x;
			}
		} catch (REngineException e) {
			throw new RException(RException.MSG_EVAL_FAILED, e);
		}
	}

	@Override
	public REXP monitoredEval(final String expr, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		checkInitialized();
		try {
			return new MonitoredEval(exec).run(expr);
		} catch (Exception e) {
			throw new RException(RException.MSG_EVAL_FAILED, e);
		}
	}

	@Override
	public void assign(final String expr, final String value) throws RException {
		checkInitialized();
		try {
			synchronized (getREngine()) {
				getREngine().assign(expr, value);
			}
		} catch (REngineException e) {
			throw new RException(RException.MSG_EVAL_FAILED, e);
		}
	}

	@Override
	public void assign(final String expr, final REXP value) throws RException {
		checkInitialized();
		try {
			synchronized (getREngine()) {
				getREngine().assign(expr, value);
			}
		} catch (REngineException e) {
			throw new RException(RException.MSG_EVAL_FAILED, e);
		}
	}

	@Override
	public void monitoredAssign(final String symbol, final REXP value, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		checkInitialized();
		try {
			new MonitoredEval(exec).assign(symbol, value);
		} catch (Exception e) {
			throw new RException(RException.MSG_EVAL_FAILED, e);
		}
	}

	@Override
	public void exportFlowVariables(final Collection<FlowVariable> inFlowVariables, final String name,
			final ExecutionMonitor exec) throws RException, CanceledExecutionException {
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

	@Override
	public void importDataFromPorts(final PortObject[] inData, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		// load workspaces from the input ports into the current R session
		for (final PortObject port : inData) {
			if (port instanceof RPortObject) {
				exec.setMessage("Loading workspace from R input port");
				final RPortObject rPortObject = (RPortObject) port;
				final File portFile = rPortObject.getFile();
				monitoredEval(
						"load(\"" + portFile.getAbsolutePath().replace('\\', '/') + "\")\n"
								+ RController.createLoadLibraryFunctionCall(rPortObject.getLibraries(), false),
						exec.createSubProgress(0.5));
			} else if (port instanceof BufferedDataTable) {
				exec.setMessage("Exporting data to R");
				// write all input data to the R session
				monitoredAssign("knime.in", (BufferedDataTable) port, exec.createSubProgress(0.5));
			}
		}

		exec.setProgress(1.0);
	}

	@Override
	public Collection<FlowVariable> importFlowVariables(final String variableName) throws RException {
		checkInitialized();
		final List<FlowVariable> flowVars = new ArrayList<FlowVariable>();
		try {
			// this used to be:
			//    getREngine().get(variableName, null, true);
			// but that caused crashes on Mac (see AP-5646) - Jonathan hasn't gotten to the bottom of it but apparently
			// it's the above commented line that kills RServe. Be more paranoid here and check existence first
			final REXP exists = getREngine().eval("exists(\"" + variableName + "\")");

			if (exists.asBytes()[0] == REXPLogical.FALSE) {
				return Collections.emptyList();
			}
			final REXP value = getREngine().eval("try(" + variableName + "\")");

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
			throw new RException("Error importing flow variables from \"" + variableName + "\"", e);
		} catch (REngineException e) {
			// the variable name was not found.
		}
		return flowVars;
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
		final int rowCount = KnowsRowCountTable.checkRowCount(table.size());

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
	private void createRListsFromBufferedDataTableColumns(final BufferedDataTable table,
			final Collection<Object> columns, final LinkedBlockingQueue<Pair<REXP, Boolean>> dest,
			final ExecutionMonitor exec) throws CanceledExecutionException {
		// NOTE: This method may not use RConnection (see
		// monitoredAssign(String, BufferedDataTable, ExecutionMonitor))

		final DataTableSpec tableSpec = table.getDataTableSpec();
		final int numColumns = tableSpec.getNumColumns();

		int c = 0;
		for (final Object columnsColumn : columns) {
			// get spec and type for current column
			final DataColumnSpec colSpec = tableSpec.getColumnSpec(c);
			final DataType type = colSpec.getType();

			exec.checkCanceled();
			exec.setProgress(c++ / (double) numColumns);

			if (type.isCollectionType()) {
				final DataType elementType = type.getCollectionElementType();
				final ArrayList<REXP> cells = new ArrayList<>();
				if (elementType.isCompatible(BooleanValue.class)) {
					final byte[][] column = (byte[][]) columnsColumn;
					for (final byte[] rowCell : column) {
						cells.add((rowCell == null) ? null : new REXPLogical(rowCell));
					}
				} else if (elementType.isCompatible(IntValue.class)) {
					final int[][] column = (int[][]) columnsColumn;
					for (final int[] rowCell : column) {
						cells.add((rowCell == null) ? null : new REXPInteger(rowCell));
					}
				} else if (elementType.isCompatible(DoubleValue.class)) {
					final double[][] column = (double[][]) columnsColumn;
					for (final double[] rowCell : column) {
						cells.add((rowCell == null) ? null : new REXPDouble(rowCell));
					}
				} else {
					final String[][] column = (String[][]) columnsColumn;
					for (final String[] rowCell : column) {
						cells.add((rowCell == null) ? null : createFactor(rowCell));
					}
				}
				// pair of the data and "isCollection" flag
				dest.add(new Pair<REXP, Boolean>(new REXPGenericVector(new RList(cells)), true));
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

				// pair of the data and "isCollection" flag
				dest.add(new Pair<REXP, Boolean>(ri, false));
			}
		}
		exec.setProgress(1.0);
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
	 * Assign the {@link #TEMP_VARIABLE_NAME} variable to a new variable with
	 * name <code>name</code>.
	 *
	 * @param name
	 *            Name for the variable
	 * @param exec
	 *            Execution monitor
	 * @throws CanceledExecutionException
	 */
	private void setVariableName(final String name, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		monitoredEval(name + "<-" + TEMP_VARIABLE_NAME + ";rm(" + TEMP_VARIABLE_NAME + ")", exec);
	}

	@Override
	public BufferedDataTable importBufferedDataTable(final String string, final boolean nonNumbersAsMissing,
			final ExecutionContext exec) throws RException, CanceledExecutionException {
		final REXP typeRexp = eval("class(" + string + ")");
		if (typeRexp.isNull()) {
			// a variable with this name does not exist
			final BufferedDataContainer cont = exec.createDataContainer(new DataTableSpec());
			cont.close();
			return cont.getTable();
		}

		try {
			final String type = typeRexp.asString();
			if (!type.equals("data.frame")) {
				throw new RException(
						"CODING PROBLEM\timportBufferedDataTable(): Supporting 'data.frame' as return type, only.");
			}
		} catch (REXPMismatchException e) {
			throw new RException("Type of " + string + " could not be parsed as string.", e);
		}

		try {
			final String[] rowIds = eval("attr(" + string + " , \"row.names\")").asStrings();

			// TODO: Support int[] as row names or int which defines the column
			// of row names:
			// http://stat.ethz.ch/R-manual/R-patched/library/base/html/row.names.html
			final int numRows = rowIds.length;
			final int ommitColumn = -1;

			final REXP value = getREngine().get(string, null, true);
			final RList rList = value.asList();

			final DataTableSpec outSpec = createSpecFromDataFrame(rList);
			final BufferedDataContainer cont = exec.createDataContainer(outSpec);

			final int numCells = ommitColumn < 0 ? rList.size() : rList.size() - 1;

			final ArrayList<DataCell>[] columns = new ArrayList[numCells];

			int cellIndex = 0; // column index without the omitted column
			for (int columnIndex = 0; columnIndex < numCells; ++columnIndex) {
				exec.checkCanceled();
				exec.setProgress(cellIndex / (double) numCells);

				if (columnIndex == ommitColumn) {
					continue;
				}

				REXP column = rList.at(columnIndex);
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
				++cellIndex;
			}

			final Iterator<DataCell>[] itors = new Iterator[numCells];
			for (int i = 0; i < numCells; ++i) {
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
		} catch (final REXPMismatchException e) {
			throw new RException("Could not parse REXP.", e);
		} catch (final REngineException e) {
			throw new RException("Could not get value of " + string + " from workspace.", e);
		}

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
			for (int r = 0; r < rexp.length(); ++r) {
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

	// --- Workspace management ---

	@Override
	public void clearWorkspace(final ExecutionMonitor exec) throws RException, CanceledExecutionException {
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

	@Override
	public List<String> clearAndReadWorkspace(final File workspaceFile, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		exec.setProgress(0.0, "Clearing previous workspace");
		clearWorkspace(exec.createSubProgress(0.3));
		exec.setMessage("Loading workspace");
		monitoredEval("load(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");",
				exec.createSubProgress(0.7));
		return importListOfLibrariesAndDelete();
	}

	@Override
	public List<String> importListOfLibrariesAndDelete() throws RException {
		try {
			final REXP listAsREXP = eval(R_LOADED_LIBRARIES_VARIABLE);
			eval("rm(" + R_LOADED_LIBRARIES_VARIABLE + ")");
			if (!listAsREXP.isVector()) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(listAsREXP.asStrings());
			}
		} catch (REXPMismatchException e) {
			LOGGER.error("Rengine error: " + e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * Class which contains the column of a table for exchange between
	 * preparation and transfer thread.
	 *
	 * @author Jonathan Hale
	 */
	private static class REXPColumn {

		private final REXP m_values;
		private final String m_name;
		private final boolean m_isCollectionType;

		/**
		 * Constructor
		 *
		 * @param name
		 *            Name of the column
		 * @param cells
		 *            List containing the cell values for R
		 * @param isCollectionType
		 *            Whether the cells are collections
		 */
		public REXPColumn(final String name, final RList cells, final boolean isCollectionType) {
			m_values = new REXPGenericVector(cells);
			m_name = name;
			m_isCollectionType = isCollectionType;
		}

		/**
		 * Constructor
		 *
		 * @param name
		 *            Name of the column
		 * @param cells
		 *            REXP containing the cell values for R
		 * @param isCollectionType
		 *            Whether the cells are collections
		 * @param list
		 */
		public REXPColumn(final String name, final REXP cells, final boolean isCollectionType) {
			m_values = cells;
			m_name = name;
			m_isCollectionType = isCollectionType;
		}

		/**
		 * @return Column name
		 */
		public String getName() {
			return m_name;
		}

		/**
		 * @return Whether the cells are collections
		 */
		public boolean isCollectionType() {
			return m_isCollectionType;
		}

		/**
		 * @return REXP containing the cell values for R
		 */
		public REXP getValues() {
			return m_values;
		}

	}

	@Override
	public void monitoredAssign(final String name, final BufferedDataTable table, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {

		final int rowCount = KnowsRowCountTable.checkRowCount(table.size());
		final String[] rowNames = new String[rowCount];

		final Collection<Object> columns = initializeAndFillColumns(table, rowNames, exec.createSubProgress(0.3));
		final LinkedBlockingQueue<Pair<REXP, Boolean>> contentQueue = new LinkedBlockingQueue<>();

		try {
			// create a new empty data.frame this is required! Without, Rserve
			// will crash with "large" amounts
			// of data.
			eval(name + "<-data.frame()");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// transfer row names to Rserve
		monitoredAssign("knime.row.names", new REXPString(rowNames), exec);
		monitoredAssign("knime.col.names", new REXPString(table.getDataTableSpec().getColumnNames()), exec);

		// script for combinding the invididual columns into a data.frame
		final StringBuilder buildScript = new StringBuilder(name + "<-data.frame(row.names=knime.row.names");
		// script for removing temporary variables
		final StringBuilder cleanupScript = new StringBuilder("rm(knime.row.names,knime.col.names");

		final int numColumns = columns.size();
		if (numColumns > 0) {
			try {
				// Task for sending columns to Rserve
				Future<REXP> future = new MonitoredEval(exec).startMonitoredThread(() -> {
					synchronized (getREngine()) {
						final RConnection conn = getREngine();

						int i = 0;
						// we expect exactly numColumns columns
						// checkCancel() is handled by the monitoring thread.
						while (i < numColumns) {
							// pair of column name and converted column data
							final Pair<REXP, Boolean> column = contentQueue.take();
							if (column == null) {
								continue;
							}

							conn.assign("c" + i, column.getFirst());
							if (column.getSecond()) {
								// We need to make sure the collections are not
								// split up into multiple columns by the
								// data.frame call. I() ensures the column is
								// taken "AsIs".
								buildScript.append(",I(c" + i + ")");
							} else {
								buildScript.append(",c" + i);
							}
							cleanupScript.append(",c" + i);
							i++;
						}
					}
					return null;
				});

				// since the following does not need the RConnection, we can safely let
				// this run in parallel with the transmission of the columns
				createRListsFromBufferedDataTableColumns(table, columns, contentQueue,
					exec.createSubProgress(0.5));

				future.get();
			} catch (InterruptedException e) {
				// no cleanup to do, node will be reset anyway.
			} catch (ExecutionException e) {
				LOGGER.error("Error while sending data to R.", e);
			}

			// build the data.frame
			monitoredEval(buildScript.append(",check.names=F);names(knime.in)<-knime.col.names").toString(), exec);

			// cleanup temporary variables
			monitoredEval(cleanupScript.append(")").toString(), exec);
		}

		exec.setProgress(1.0);
	}

	@Override
	public void saveWorkspace(final File workspaceFile, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		// save workspace to file
		monitoredEval("save.image(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");", exec);
	}

	@Override
	public void loadLibraries(final List<String> listOfLibraries) throws RException {
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
	public static String createLoadLibraryFunctionCall(final List<String> listOfLibraries,
			final boolean suppressMessages) {
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

	// --- Monitored Evaluation helpers ---

	/**
	 * Evaluation of R code with a monitor in a separate thread to cancel the
	 * code execution in case the execution of the node is cancelled.
	 */
	private final class MonitoredEval {

		private final int m_interval = 200;
		private final ExecutionMonitor m_exec;

		/**
		 * Constructor
		 *
		 * @param exec
		 *            for tracking progress and checking cancelled state.
		 */
		public MonitoredEval(final ExecutionMonitor exec) {
			m_exec = exec;
		}

		/*
		 * Run the Callable in a thread and make sure to cancel it, in case
		 * execution is cancelled.
		 */
		private REXP monitor(final Callable<REXP> task) {
			final FutureTask<REXP> runningTask = new FutureTask<>(task);
			final Thread t = (m_useNodeContext) ? ThreadUtils.threadWithContext(runningTask, "R-Evaluation")
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
		 *             when execution was cancelled
		 * @throws IOException
		 *             when initialization of R or Rserve failed when attempting
		 *             to recover
		 */
		public REXP run(final String cmd)
				throws REngineException, REXPMismatchException, CanceledExecutionException, Exception {
			final Future<REXP> future = startMonitoredThread(() -> {
				return eval(cmd);
			});

			try {
				// wait for evaluation to complete
				return future.get();
			} catch (InterruptedException | ExecutionException e) {
				return null;
			} finally {
				// Make sure to recover in case user terminated or crashed our
				// server
				checkConnectionAndRecover();
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
		 * @throws Exception
		 */
		public void assign(final String symbol, final REXP value)
				throws REngineException, REXPMismatchException, CanceledExecutionException, Exception {
			final Future<REXP> future = startMonitoredThread(() -> {
				synchronized (getREngine()) {
					getREngine().assign(symbol, value);
				}
				return null;
			});

			try {
				// wait for evaluation to complete
				future.get();
			} catch (InterruptedException | ExecutionException e) {
			} finally {
				// Make sure to recover in case user terminated or crashed our
				// server
				checkConnectionAndRecover();
			}
		}

		/*
		 * Execute a Callable in a monitored thread
		 */
		public Future<REXP> startMonitoredThread(final Callable<REXP> task) {
			final FutureTask<REXP> ret = new FutureTask<REXP>(() -> {
				return monitor(task);
			});

			if (m_useNodeContext) {
				ThreadUtils.threadWithContext(ret, "R-Monitor").start();
			} else {
				new Thread(ret, "R-Monitor").start();
			}
			return ret;
		}

	}

}