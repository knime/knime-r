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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
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
import org.knime.core.util.ThreadPool;
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

	/** Name of the R variable which stores the names of loaded R libraries */
	public static final String R_LOADED_LIBRARIES_VARIABLE = "knime.loaded.libraries";

	private RConnectionResource m_connection;

	private Properties m_rProps;

	private boolean m_initialized = false;
	private boolean m_useNodeContext = false;

	private static boolean quartzFound;

	private static boolean cairoFound;

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
	 */
	public synchronized void terminateAndRelaunch() {
		LOGGER.debug("Terminating R process");

		terminateRProcess();

		try {
			m_connection = initRConnection();
			m_initialized = (m_connection != null && m_connection.get().isConnected());
			LOGGER.debug("Recovered with a new R process");
		} catch (Exception e) {
			throw new RuntimeException("Initializing R with Rserve failed.", e);
		}
	}

	/**
	 * Terminate the R process started for this RController
	 */
	// public because called from test, otherwise this is 'private'
	public synchronized void terminateRProcess() {
		if (m_connection != null) {
			m_connection.destroy(true);
			m_connection = null;
		}

		m_initialized = false;
	}

	/**
	 * Check if the connection is still valid and recover if not.
	 */
	public synchronized void checkConnectionAndRecover() {
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
						"Cannot determine major version of R. Please check the R installation defined in the KNIME preferences.", null);
			}

			final String rserveProp = m_rProps.getProperty("Rserve.path");
			if (rserveProp == null || rserveProp.isEmpty()) {
				org.knime.ext.r.bin.preferences.RPreferenceInitializer.invalidatePreferenceProviderCache();
				throw new RException(
						"Could not find Rserve package. Please install it in your R installation by running \"install.packages('Rserve')\".", null);
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
				eval("memory.limit(" + rMemoryLimit + ");", false);
			} catch (Exception e) {
				LOGGER.error("R initialisation failed. " + e.getMessage());
				throw new RuntimeException(e);
			}
		} else if (Platform.isMac()) {
			checkCairoOnMac();
		}
	}

	private void checkCairoOnMac() throws RException {
		if (cairoFound && quartzFound) {
			return;
		}

		// produce a warning message if 'Cairo' package is not installed.
		try {
			final REXP ret = eval("find.package('Cairo')", true);
			final String cairoPath = ret.asString();

			if (!StringUtils.isEmpty(cairoPath)) {
				// under Mac we need Cairo package to use png()/bmp() etc devices.
				cairoFound = true;
			}
		} catch (RException | REXPMismatchException e) {
			LOGGER.debug("Error while querying Cairo package version: "+ e.getMessage(), e);
		}

		if (!cairoFound) {
			LOGGER.warn("The package 'Cairo' needs to be installed in your R installation for bitmap graphics "
					+ "devices to work properly. Please install it in R using \"install.packages('Cairo')\".");
			return;
		}

		// Cairo requires XQuartz to be installed. We make sure it is, since
		// loading the Cairo library will crash Rserve otherwise.
		final ProcessBuilder builder = new ProcessBuilder("mdls", "-name", "kMDItemVersion",
				"/Applications/Utilities/XQuartz.app");

		try {
			final Process process = builder.start();

			// check if output of process was a valid version
			final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stdout.readLine()) != null) {
				if (line.matches("kMDItemVersion = \"2(?:\\.[0-9]+)+\"")) {
					quartzFound = true;
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
			LOGGER.error("Could not run mdls to check for XQuartz version: " + e.getMessage(), e);
		}

		if (!quartzFound) {
			throw new RException("XQuartz is required for the Cairo library on MacOS. Please download "
					+ "and install XQuartz from http://www.xquartz.org/.", null);
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
	public REXP eval(final String expr, final boolean resolve) throws RException {
		try {
			synchronized (getREngine()) {
				// sadly, eval(String, RExpr, boolean) has a bug and just completely ignores the "resolve" parameter. Immitating its behaviour here.
				if (resolve) {
					REXP x = getREngine().eval(expr);
					return x;
				} else {
					getREngine().voidEval(expr);
					return null;
				}
			}
		} catch (REngineException e) {
			throw new RException(RException.MSG_EVAL_FAILED + ": \"" + expr + "\"", e);
		}
	}

	@Override
	public REXP monitoredEval(final String expr, final ExecutionMonitor exec, final boolean resolve)
			throws RException, CanceledExecutionException, InterruptedException {
		checkInitialized();
		try {
			return new MonitoredEval(exec).run(expr, resolve);
		} catch (RException | REngineException | REXPMismatchException e) {
			throw new RException(RException.MSG_EVAL_FAILED + ": \"" + expr + "\"", e);
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
			throw new RException(RException.MSG_EVAL_FAILED + ": \"" + expr + "\"", e);
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
			throw new RException(RException.MSG_EVAL_FAILED + ": \"" + expr + "\"", e);
		}
	}

	@Override
	public void monitoredAssign(final String symbol, final REXP value, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		checkInitialized();
		try {
			new MonitoredEval(exec).assign(symbol, value);
		} catch (Exception e) {
			throw new RException(String.format("Assigning value to %s failed.", symbol), e);
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
		eval(name + "<-" + TEMP_VARIABLE_NAME + ";rm(" + TEMP_VARIABLE_NAME + ")", false);
	}

	@Override
	public void importDataFromPorts(final PortObject[] inData, final ExecutionMonitor exec, final int batchSize, final String rType, final boolean sendRowNames)
			throws RException, CanceledExecutionException {
		// load workspaces from the input ports into the current R session
		for (final PortObject port : inData) {
			if (port instanceof RPortObject) {
				exec.setMessage("Loading workspace from R input port");
				final RPortObject rPortObject = (RPortObject) port;
				final File portFile = rPortObject.getFile();
				eval("load(\"" + portFile.getAbsolutePath().replace('\\', '/') + "\")\n"
						+ RController.createLoadLibraryFunctionCall(rPortObject.getLibraries(), false),
						false);
			} else if (port instanceof BufferedDataTable) {
				exec.setMessage("Exporting data to R");
				// write all input data to the R session
				monitoredAssign("knime.in", (BufferedDataTable) port, exec.createSubProgress(0.5), batchSize, rType, sendRowNames);
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
			final REXP value = getREngine().eval("try(" + variableName + ")");

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

	@Override
	public BufferedDataTable importBufferedDataTable(final String varName, final boolean nonNumbersAsMissing,
			final ExecutionContext exec) throws RException, CanceledExecutionException {

		final REXP typeRexp = eval("class(" + varName + ")", true);
		if (typeRexp.isNull()) {
			// a variable with this name does not exist
			final BufferedDataContainer cont = exec.createDataContainer(new DataTableSpec());
			cont.close();
			return cont.getTable();
		}

		boolean isDataTable = false;
		try {
			final String type = typeRexp.asString();
			if (type.equals("data.table")) {
				isDataTable = true;
				LOGGER.warn("Using experimental support for receiving data as \"data.table\".");
			} else if (type.equals("matrix") || type.equals("list")) {
				eval(varName + "<-data.frame(" + varName + ")", false);
			} else if (!type.equals("data.frame")) {
				throw new RException(
						"CODING PROBLEM\timportBufferedDataTable(): Supporting only 'data.frame', 'data.table', 'matrix' and 'list' for type of \"" + varName  +"\" (was '" + type + "').", null);
			}
		} catch (REXPMismatchException e) {
			throw new RException("Type of " + varName + " could not be parsed as string.", e);
		}

		ThreadPool threadPool = ThreadPool.currentPool();

		try {
			// Get column names
			final String[] columnNames = eval("colnames(" + varName + ")", true).asStrings();
			final int numColumns = columnNames.length;

			// Get row count (and row names if not automatic compact 1:n storage)
			final REXP numRowsRexp = getREngine().eval(".row_names_info(" + varName + ")");
			final boolean compactRowNames = numRowsRexp.asInteger() < 0;
			if(!compactRowNames) {
				eval("knime.out.row.names<-attr(" + varName + ",\"row.names\")", false);
			}
			final int numRows = Math.abs(numRowsRexp.asInteger());
			int transferredRows = 0;

			final List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
			DataTableSpec outSpec = null;
			BufferedDataContainer cont = null;

			final DataCell[][] columns = new DataCell[numColumns][];
			final Future<Void>[] futures = new Future[numColumns];
			Future<Void> addRowsFuture = null;

			while (transferredRows < numRows) {
				int rowsThisBatch = Math.min(50000, numRows-transferredRows);

				if (numRows - transferredRows - rowsThisBatch < 10000) {
					// avoid final chunk being smaller that 100k rows
					rowsThisBatch = numRows - transferredRows;
				}

				// Expression for range of rows to transfer e.g.: 1:10000
				final String rowRangeExpr = (transferredRows+1) + ":" + (transferredRows+rowsThisBatch);

				for (int i = 0; i < numColumns; ++i) {
					exec.checkCanceled();
					exec.setProgress((transferredRows/(double)numRows)+(rowsThisBatch/(double)numRows)*(i/(double)numColumns));

					final int rIndex = i+1; // R starts indices at 1
					final String expr = varName + "[" + rowRangeExpr + "," + rIndex + "]" + ((isDataTable) ? "[[1]]" : "");
					final REXP column = getREngine().eval(expr);

					if(outSpec == null) {
						// Create column spec for this column
						DataType colType = null;
						if (column.isNull()) {
							colType = StringCell.TYPE;
						} else if (column.isList()) {
							colType = DataType.getType(ListCell.class, DataType.getType(DataCell.class));
						} else {
							colType = importDataType(column);
						}
						colSpecs.add(new DataColumnSpecCreator(columnNames[i], colType).createSpec());
					}

					// Convert values
					if (columns[i] == null || columns[i].length < rowsThisBatch) {
						// Only reallocate the DataCell buffer if insufficient size.
						columns[i] = new DataCell[rowsThisBatch];
					}

					if (addRowsFuture != null) {
						try {
							addRowsFuture.get();
						} catch (Throwable e) {
							new RuntimeException("Error while adding rows to table.", e);
						}
					}

					if (column.isNull()) {
						Arrays.fill(columns[i], DataType.getMissingCell());
					} else {
						if (column.isList()) {
							final DataCell[] list = columns[i];
							int row = 0;
							for (final Object o : column.asList()) {
								final REXP rexp = (REXP) o;
								if (rexp.isNull()) {
									list[row] = DataType.getMissingCell();
								} else {
									if (rexp.isVector()) {
										final REXPVector colValue = (REXPVector) rexp;
										final DataCell[] listCells = new DataCell[colValue.length()];
										importCells(colValue, listCells, nonNumbersAsMissing);
										list[row] = CollectionCellFactory.createListCell(Arrays.asList(listCells));
									} else {
										LOGGER.warn("Expected Vector type for list cell. Inserting missing cell instead.");
										list[row] = DataType.getMissingCell();
									}
								}
								++row;
							}
						} else {
							final DataCell[] columnCells = columns[i];
							Callable<Void> callable = () -> {
								importCells(column, columnCells, nonNumbersAsMissing);
								return null;
							};
                            futures[i] = threadPool != null ? threadPool.enqueue(callable)
                                : R_THREAD_POOL.submit(callable);
						}
					}
				}

				if (outSpec == null || cont == null) {
					// create container and outspec for the first batch of rows
					outSpec = new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
					cont = exec.createDataContainer(outSpec);
				}

				Stream.of(futures).filter(o -> o != null).forEach(f -> {
					try {
						f.get();
					} catch (final Throwable e) {
						throw new RuntimeException("Error during conversion of R values to KNIME types.", e);
					}
				});

				final REXP rRowIds = (compactRowNames) ? null : getREngine().eval("knime.out.row.names[" + rowRangeExpr + "]");

				final DataCell[] curRow = new DataCell[numColumns];

				final BufferedDataContainer finalCont = cont;
				final int finalTransferredRows = transferredRows;
				final int finalRowsThisBatch = rowsThisBatch;
				addRowsFuture = R_THREAD_POOL.submit(() -> {
					if (compactRowNames) {
						for(int i = 0; i < finalRowsThisBatch; ++i) {
							for (int col = 0; col < columns.length; ++col) {
								curRow[col] = columns[col][i];
							}
							finalCont.addRowToTable(new DefaultRow(Integer.toString(1 + i + finalTransferredRows), curRow));
						}
					} else {
						final String[] rowIds = rRowIds.asStrings();

						for(int i = 0; i < finalRowsThisBatch; ++i) {
							for (int col = 0; col < columns.length; ++col) {
								curRow[col] = columns[col][i];
							}
							finalCont.addRowToTable(new DefaultRow(rowIds[i + finalTransferredRows], curRow));
						}
					}
					return null;
				});

				transferredRows += rowsThisBatch;
			}

			if (addRowsFuture != null) {
				try {
					addRowsFuture.get();
				} catch (Throwable e) {
					new RuntimeException("Error while adding rows to table.", e);
				}
			}

			if (outSpec == null || cont == null) {
				// create container and outspec for the first batch of rows
				outSpec = new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
				cont = exec.createDataContainer(outSpec);
			}
			cont.close();

			return cont.getTable();
		} catch (final REXPMismatchException e) {
			throw new RException("Could not parse REXP.", e);
		} catch (final REngineException e) {
			throw new RException("Could not get value of " + varName + " from workspace.", e);
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
	private final void importCells(final REXP rexp, final DataCell[] column, final boolean nonNumbersAsMissing)
			throws REXPMismatchException {
		if (rexp.isLogical()) {
			final byte[] bytes = rexp.asBytes();
			for (int i = 0; i < bytes.length; ++i) {
				final byte val = bytes[i];
				if (val == REXPLogical.TRUE) {
					column[i] = BooleanCell.TRUE;
				} else if (val == REXPLogical.FALSE) {
					column[i] = BooleanCell.FALSE;
				} else {
					column[i] = DataType.getMissingCell();
				}
			}
		} else if (rexp.isFactor()) {
			final RFactor strings = rexp.asFactor();
			for (int r = 0; r < strings.size(); ++r) {
				final String colValue = strings.at(r);
				column[r] = (colValue == null) ? DataType.getMissingCell() : new StringCell(colValue);
			}
		} else if (rexp.isInteger()) {
			final int[] ints = rexp.asIntegers();
			for (int i = 0; i < ints.length; ++i) {
				final int val = ints[i];
				column[i] = (val == REXPInteger.NA) ? DataType.getMissingCell() : new IntCell(val);
			}
		} else if (rexp.isNumeric()) {
			double[] doubles = rexp.asDoubles();
			for (int i = 0; i < doubles.length; ++i) {
				final double val = doubles[i];
				if (!REXPDouble.isNA(val) && !(nonNumbersAsMissing && (Double.isNaN(val) || Double.isInfinite(val)))) {
					/*
					 * If R value is not NA (not available), missing cell will
					 * be exported instead. Also, if nonNumbers should be
					 * exported as missing cells, NaN and Infinite will be
					 * exported as missing cells instead, aswell.
					 */
					column[i] = new DoubleCell(val);
				} else {
					column[i] = DataType.getMissingCell();
				}
			}
		} else {
			final String[] strings = rexp.asStrings();
			for (int i = 0; i < strings.length; ++i) {
				final String val = strings[i];
				column[i] = (val == null) ? DataType.getMissingCell() : new StringCell(val);
			}
		}
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
		try {
			monitoredEval(b.toString(), exec, false);
		} catch (InterruptedException e) {
			throw new RException("Interrupted while loading R workspace.", e);
		}
		exec.setProgress(1.0, "Clearing previous workspace");
	}

	@Override
	public List<String> clearAndReadWorkspace(final File workspaceFile, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		exec.setProgress(0.0, "Clearing previous workspace");
		clearWorkspace(exec.createSubProgress(0.3));
		exec.setMessage("Loading workspace");
		try {
			monitoredEval("load(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");",
					exec.createSubProgress(0.7), false);
		} catch (InterruptedException e) {
			throw new RException("Interrupted while loading R workspace.", e);
		}
		return importListOfLibrariesAndDelete();
	}

	@Override
	public List<String> importListOfLibrariesAndDelete() throws RException {
		try {
			final REXP listAsREXP = eval(R_LOADED_LIBRARIES_VARIABLE, true);
			eval("rm(" + R_LOADED_LIBRARIES_VARIABLE + ")", false);
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

	/** Map of type to a R expression which creates a column vector for that type */
	private static final Map<Class<? extends DataValue>, String> DATA_TYPE_TO_R_CONSTRUCTOR;

    private static final ExecutorService R_THREAD_POOL =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	static {
		Map<Class<? extends DataValue>, String> tmp = new HashMap<>();

		tmp.put(IntValue.class, "integer(rowCount)");
		tmp.put(BooleanValue.class, "logical(rowCount)");
		tmp.put(DoubleValue.class, "double(rowCount)");
		tmp.put(StringValue.class, "character(rowCount)");
		tmp.put(CollectionDataValue.class, "vector(mode='list', length=rowCount)");

		DATA_TYPE_TO_R_CONSTRUCTOR = Collections.unmodifiableMap(tmp);
	}

	/* This class ties together all variables concerning a single batch */
	private static class Batch {
		final int size; /** Size of this batch */
		int index = 0; /** Current row index for writing to */

		final RList rBatch; // List containing rColumns
		final REXPString rRowNames;
		final REXPGenericVector rVector;

		/**
		 * @param numRows Number of rows for this batch.
		 * @param columnCount Number of columns.
		 */
		public Batch(final int numRows, final int columnCount) {
			this.size = numRows;

			rRowNames = new REXPString(new String[numRows]);
			rBatch = new RList(columnCount+1, false);
			rVector = new REXPGenericVector(rBatch);
		}
	}

	@Override
	public void monitoredAssign(final String name, final BufferedDataTable table, final ExecutionMonitor exec, final int batchSize, final String rType, final boolean sendRowNames)
			throws RException, CanceledExecutionException {

		final int rowCount = KnowsRowCountTable.checkRowCount(table.size());
		final int columnCount = table.getDataTableSpec().getNumColumns();

		assign("rowCount", new REXPInteger(rowCount));
		assign("colCount", new REXPInteger(columnCount));

		@SuppressWarnings("unchecked")
		final Class<? extends DataValue>[] dataValueClasses = new Class[columnCount]; // type of each column

		/*
		 * Allocate the memory for the columns on R side.
		 * They will be concatenated to a data.frame or data.table after filled with input data (without copying the memory)
		 */
		exec.setMessage("Allocating memory for R columns.");

		// Create cols variable (array of column vectors), will be coerced to data.frame later.
		eval("cols<-list(length=colCount)", false);

		// Script for removing temporary variables
		final StringBuilder cleanupScript = new StringBuilder("rm(knime.row.names,knime.col.names,bt,i,rowCount,colCount,cols");

		// script for combining the individual columns into a data.frame (or data.table)
		final boolean useDataTable = "data.table".equals(rType);
		if (useDataTable) {
			final REXP ret = eval("find.package('data.table')", true);
			try {
				if(StringUtils.isEmpty(ret.asString())) {
					throw new RuntimeException("Selected data.table as type for \"" + name + "\", but package could not be found.");
				}
			} catch (REXPMismatchException e) {
				throw new IllegalStateException("\"find.package\" doesn't return string anymore.", e);
			}
			LOGGER.warn("Using experimental support for sending data as \"data.table\".");
		}

		// Variables concerning a single batch
		final Batch batch = new Batch(Math.min(batchSize, rowCount), columnCount);

		// Get type of each column and generate R code which creates an appropriate column
		int columnIndex = 0;
		for (final DataColumnSpec columnSpec : table.getDataTableSpec()) {
			final String columnVar = "cols[[" + (columnIndex+1) + "]]";

			final DataType type = columnSpec.getType();
			Class<? extends DataValue> dataValueClass;
			if (type.isCollectionType()) {
				dataValueClass = CollectionDataValue.class;
			} else {
				if (type.isCompatible(BooleanValue.class)) {
					dataValueClass = BooleanValue.class;
				} else if (type.isCompatible(IntValue.class)) {
					dataValueClass = IntValue.class;
				} else if (type.isCompatible(DoubleValue.class)) {
					dataValueClass = DoubleValue.class;
				} else {
					dataValueClass = StringValue.class;
				}
			}
			dataValueClasses[columnIndex] = dataValueClass;
			final String constructor = DATA_TYPE_TO_R_CONSTRUCTOR.get(dataValueClass);
			eval(columnVar + "<-I(" + constructor + ")", false); // Allocate vector for column, e.g.: c10 <- double(12345)

			// Prepare KNIME side batch for this column
			if (dataValueClass == CollectionDataValue.class) {
				final RList col = new RList(batch.size, false);
				for (int r = 0; r < batch.size; ++r) {
					col.add(null);
				}
				batch.rBatch.add(new REXPGenericVector(col));
			} else if (dataValueClass == BooleanValue.class) {
				batch.rBatch.add(new REXPLogical(new byte[batch.size]));
			} else if (dataValueClass == IntValue.class) {
				batch.rBatch.add(new REXPInteger(new int[batch.size]));
			} else if (dataValueClass == DoubleValue.class) {
				batch.rBatch.add(new REXPDouble(new double[batch.size]));
			} else {
				batch.rBatch.add(new REXPString(new String[batch.size]));
			}

			columnIndex++;

			exec.checkCanceled(); // Useful for many many rows
		}

		if (sendRowNames) {
			// Allocate vector for row names on R side
			eval("knime.row.names<-character(rowCount)", false);
			cleanupScript.append(",knime.row.names");

			// And on KNIME side
			batch.rBatch.add(batch.rRowNames);
		}

		exec.setMessage("Sending column names.");
		// transfer column names to Rserve
		monitoredAssign("knime.col.names", new REXPString(table.getDataTableSpec().getColumnNames()), exec);

		if (useDataTable) {
			// Create data.table now, we will use set(table, column, row, newData) to set values in the table directly
			try {
				monitoredEval("library(data.table);" + name + "<-as.data.table(cols, check.names=F);names(" + name + ")<-knime.col.names", exec, false);
			} catch (InterruptedException e) {
				throw new RException("Interrupted while creating data.table and assigning column names.", e);
			}
		}

		/*
		 * Send rows to R in batches
		 */
		exec.setMessage("Sending rows to R.");

		final double numRows = table.size(); // for progress reporting only
		long rowIndex = 0;

		// variables for "Rows per second" debug output
		long timeSinceUpdate = System.currentTimeMillis();
		int rowsSinceUpdate = 0;

		for (final DataRow row : table) {

			// The following block prints the amount of rows sent every second for a rough estimate while benchmarking
			if (System.currentTimeMillis() - timeSinceUpdate > 1000) {
				LOGGER.debugWithFormat("Rows per second: %d", rowsSinceUpdate);
				rowsSinceUpdate = 0;
				timeSinceUpdate = System.currentTimeMillis();
			} else {
				++rowsSinceUpdate;
			}

			if (sendRowNames) {
				batch.rRowNames.asStrings()[batch.index] = row.getKey().getString();
			}

			// Assign the values of the current row to the batch
			int c = 0; // columnIndex
			for (final DataCell cell : row) {
				final Class<? extends DataValue> type = dataValueClasses[c];

				try {
					if (type == CollectionDataValue.class) {
						REXP value;
						// try get value from collection cell
						if (cell.isMissing()) {
							value = null;
						} else {
							final CollectionDataValue collValue = (CollectionDataValue) cell;
							final DataType elementType = cell.getType().getCollectionElementType();
							if (elementType.isCompatible(BooleanValue.class)) {
								final byte[] elementValue = new byte[collValue.size()];
								int i = 0;
								for (final DataCell e : collValue) {
									if (e.isMissing()) {
										elementValue[i] = REXPLogical.NA;
									} else {
										elementValue[i] = ((BooleanValue)e).getBooleanValue() ? REXPLogical.TRUE : REXPLogical.FALSE;
									}
									++i;
								}
								value = new REXPLogical(elementValue);

							} else if (elementType.isCompatible(IntValue.class)) {
								final int[] elementValue = new int[collValue.size()];
								int i = 0;
								for (final DataCell e : collValue) {
									elementValue[i] = ((IntValue)e).getIntValue();
									++i;
								}
								value = new REXPInteger(elementValue);
							} else if (elementType.isCompatible(DoubleValue.class)) {
								final double[] elementValue = new double[collValue.size()];
								int i = 0;
								for (final DataCell e : collValue) {
									elementValue[i] = ((DoubleValue)e).getDoubleValue();
									++i;
								}
								value = new REXPDouble(elementValue);
							} else {
								final String[] elementValue = new String[collValue.size()];
								int i = 0;
								for (final DataCell e : collValue) {
									elementValue[i] = ((StringCell)e).getStringValue();
									++i;
								}
								value = new REXPString(elementValue);
							}
						}
						((REXP)batch.rBatch.get(c)).asList().set(batch.index, value);
					} else {
						final REXP curREXP = (REXP)batch.rBatch.get(c);
						if (type.equals(BooleanValue.class)) {
							curREXP.asBytes()[batch.index] = exportBooleanValue(cell);
						} else if (type.equals(IntValue.class)) {
							curREXP.asIntegers()[batch.index] = exportIntValue(cell);
						} else if (type.equals(DoubleValue.class)) {
							curREXP.asDoubles()[batch.index] = exportDoubleValue(cell);
						} else {
							curREXP.asStrings()[batch.index] = exportStringValue(cell);
						}
					}
				} catch (REXPMismatchException e) {
					// Will never happen, the REXPs types are added according to column types.
					throw new IllegalStateException(e);
				}
				++c;
			}

			++batch.index;
			if (batch.index == batch.size || batch.index+rowIndex == rowCount) {
				// Batch full or end of table
				assign("bt", batch.rVector);

				final long start = rowIndex + 1;
				final long end = rowIndex + batch.index;

				if (useDataTable) {
					eval("for(i in 1:colCount){set(" + name + "," + start + ":" + end + ",i,bt[i])}", false);
				} else {
					eval("for(i in 1:colCount){cols[[i]][" + start + ":" + end + "]<-bt[[i]][1:" + batch.index + "]}", false);
				}

				if (sendRowNames) {
					eval("knime.row.names[" + start + ":" + end + "]<-bt[[colCount+1]][1:" + batch.index + "]", false);
				}

				// Not relevant if batch.index+rowIndex == rowCount
				rowIndex += batch.size;
				batch.index = 0;

				exec.checkCanceled();
				exec.setProgress(rowIndex / numRows);
			}
		}

		try {
			if (useDataTable) {
				// Assign row names if sent
				if (sendRowNames) {
					monitoredEval("row.names(" + name + ")<-knime.row.names", exec, false);
				}
			} else {
				// Coerce columns to data.frame (rather than constructing a new one which would copy the entire data)
				if (sendRowNames) {
					monitoredEval(name + "<-as.data.frame(cols,row.names=knime.row.names,check.names=F);names(" + name + ")<-knime.col.names", exec, false);
				} else {
					monitoredEval(name + "<-as.data.frame(cols,check.names=F);names(" + name + ")<-knime.col.names", exec, false);
				}
			}
		} catch (InterruptedException e) {
			throw new RException("Interrupted while setting row names or creating data.frame.", e);
		}

		/* Clean up */
		exec.setMessage("Cleaning up.");

		eval(cleanupScript.append(")").toString(), false);

		exec.setProgress(1.0);
	}

	@Override
	public void saveWorkspace(final File workspaceFile, final ExecutionMonitor exec)
			throws RException, CanceledExecutionException {
		// save workspace to file
		try {
			monitoredEval("save.image(\"" + workspaceFile.getAbsolutePath().replace('\\', '/') + "\");", exec, false);
		} catch (InterruptedException e) {
			throw new RException("Interrupted while saving R workspace.", e);
		}
	}

	@Override
	public void loadLibraries(final List<String> listOfLibraries) throws RException {
		final String cmd = createLoadLibraryFunctionCall(listOfLibraries, true);
		eval(cmd, false);
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
		private REXP monitor(final Callable<REXP> task) throws InterruptedException, RException, CanceledExecutionException {
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
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RException) {
					throw (RException) e.getCause();
				}
				throw new RException("Exception during R evaluation", e);
			} finally {
				try {
					if (t.isAlive()) {
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
		}

		/**
		 * Run R code
		 *
		 * @param cmd The R command to run
		 * @param resolve Whether to resolve the resulting reference
		 * @return Result of the code (if resolve is true) or a reference to the result (if resolve is false)
		 * @throws REngineException
		 * @throws RException
		 * @throws REXPMismatchException
		 * @throws CanceledExecutionException when execution was cancelled
		 * @throws InterruptedException
		 */
		public REXP run(final String cmd, final boolean resolve)
				throws REngineException, REXPMismatchException, RException, CanceledExecutionException, InterruptedException {
			try {
				// wait for evaluation to complete
				return monitor(() -> {
					return eval(cmd, resolve);
				});
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
			try {
				// wait for evaluation to complete
				monitor(() -> {
					synchronized (getREngine()) {
						getREngine().assign(symbol, value);
					}
					return null;
				});
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