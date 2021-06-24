/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.r.controller;

import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.knime.core.data.RowKey;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadPool;
import org.knime.core.util.ThreadUtils;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.RBinUtil.InvalidRHomeException;
import org.knime.ext.r.bin.preferences.DefaultRPreferenceProvider;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
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
 * This class manages some way of communicating with R, executing R code and moving data back and forth.
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

    private final RPreferenceProvider m_preferences;

    private RConnectionResource m_connection;

    private Properties m_rProps;

    private boolean m_initialized = false;

    private boolean m_useNodeContext = false;

    /**
     * Constructor. Calls {@link #initialize()}. To avoid initialization, use {@link #RController(boolean)}.
     *
     * @deprecated use {@link #RController(boolean)} or {@link #RController(boolean, RPreferenceProvider)} with
     *             <code>useNodeContext=false</code> and call {@link #initialize()}.
     * @throws RException
     */
    @Deprecated
    public RController() throws RException {
        // Deprecated to clean up the API. It is confusing if some constructors call initialize and some don't.
        // Especially with the added preferences parameter.
        this(false);
        initialize();
    }

    /**
     * Creates a new {@link RController} for the default R preferences. The controller is not yet initialized. Call
     * {@link #initialize()} before using it.
     *
     * @param useNodeContext Whether to use the NodeContext for threads
     */
    public RController(final boolean useNodeContext) {
        this(useNodeContext, getDefaultRPreferences());
    }

    /**
     * Creates a new {@link RController} for the given R preferences. The controller is not yet initialized. Call
     * {@link #initialize()} before using it.
     *
     * @param useNodeContext Whether to use the NodeContext for threads
     * @param preferences the R preferences to use
     */
    public RController(final boolean useNodeContext, final RPreferenceProvider preferences) {
        m_preferences = preferences;
        setUseNodeContext(useNodeContext);
    }

    private static RPreferenceProvider getDefaultRPreferences() {
        return RPreferenceInitializer.getRProvider();
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
     * Check if the RController is initialized and throws {@link RControllerNotInitializedException} if not.
     */
    private final void checkInitialized() {
        if (!m_initialized || (m_connection == null)) {
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
     * Terminate and relaunch the R process this controller is connected to. This is currently the only way to interrupt
     * command execution.
     */
    public synchronized void terminateAndRelaunch() {
        LOGGER.debug("Terminating R process");

        terminateRProcess();

        try {
            m_connection = initRConnection(m_preferences);
            m_initialized = ((m_connection != null) && m_connection.get().isConnected());
            LOGGER.debug("Recovered with a new R process");
        } catch (final Exception e) {
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
        if ((m_connection != null) && m_connection.get().isConnected() && m_connection.isRInstanceAlive()) {
            // connection is fine.
            return;
        }

        // all of the session data has been lost. We cannot recover from that.
        terminateAndRelaunch();
    }

    /** Create and initialize a R connection */
    private static RConnectionResource initRConnection(final RPreferenceProvider preferences) throws RserveException, IOException {
        final RConnectionResource resource = RConnectionFactory.createConnection(preferences);

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
            RBinUtil.checkRHome(m_preferences);

            // Use cached preferences if DefaultRPreferenceProvider
            if (m_preferences instanceof DefaultRPreferenceProvider) {
                m_rProps = ((DefaultRPreferenceProvider)m_preferences).getProperties();
            } else {
                m_rProps = RBinUtil.retrieveRProperties(m_preferences);
            }

            if (!m_rProps.containsKey("major")) {
                throw new RException(
                    "Cannot determine major version of R. Please check the R installation defined in the KNIME preferences.",
                    null);
            }

            if (!RBinUtil.checkRServeInstalled(m_rProps)) {
                invalidatePrefCacheIfDefaultR();
                throw new RException(
                    "Could not find Rserve package. Please install it in your R installation by running "
                        + "\"install.packages('Rserve',,'http://rforge.net/',type='source')\".",
                    null);
            }


            m_connection = initRConnection(m_preferences);
        } catch (final InvalidRHomeException ex) {
            throw new RException("R Home \"" + m_preferences.getRHome() + "\" is invalid.", ex);
        } catch (final RserveException | IOException e) {
            throw new RException("Exception occured during R initialization.", e);
        }

        m_initialized = ((m_connection != null) && m_connection.get().isConnected());

        if (Platform.isWindows()) {
            try {
                final String rMemoryLimit = m_rProps.get("memory.limit").toString().trim();
                // set memory to the one of the used R
                eval("memory.limit(" + rMemoryLimit + ");", false);
            } catch (final Exception e) {
                LOGGER.error("R initialisation failed. " + e.getMessage());
                throw new RuntimeException(e);
            }
        } else if (Platform.isMac()) {
            RCairoChecker.checkCairoOnMac(m_preferences, this);
        }

        if (!RBinUtil.checkRServeAndRVersion(m_rProps)) {
            LOGGER.warn("R Version >= 3.5.0 and Rserve < 1.8-6 currently have issues preventing their full use in KNIME. "
                + "A future release of R and/or Rserve may fix these issues.");
        }
    }

    /** If this RController uses the default prefs: Invalidate the cache to recompute properties */
    private void invalidatePrefCacheIfDefaultR() {
        if (m_preferences.equals(RPreferenceInitializer.getRProvider())) {
            RPreferenceInitializer.invalidatePreferenceProviderCache();
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
                    final REXP x = getREngine().eval(expr);
                    return x;
                } else {
                    getREngine().voidEval(expr);
                    return null;
                }
            }
        } catch (final REngineException e) {
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
        } catch (final REngineException e) {
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
        } catch (final REngineException e) {
            throw new RException(RException.MSG_EVAL_FAILED + ": \"" + expr + "\"", e);
        }
    }

    @Override
    public void monitoredAssign(final String symbol, final REXP value, final ExecutionMonitor exec)
        throws RException, CanceledExecutionException {
        checkInitialized();
        try {
            new MonitoredEval(exec).assign(symbol, value);
        } catch (final Exception e) {
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
    public void importDataFromPorts(final PortObject[] inData, final ExecutionMonitor exec, final int batchSize,
        final String rType, final boolean sendRowNames) throws RException, CanceledExecutionException {
        // load workspaces from the input ports into the current R session
        for (final PortObject port : inData) {
            if (port instanceof RPortObject) {
                exec.setMessage("Loading workspace from R input port");
                final RPortObject rPortObject = (RPortObject)port;
                final File portFile = rPortObject.getFile();
                eval("load(\"" + portFile.getAbsolutePath().replace('\\', '/') + "\")\n"
                    + RController.createLoadLibraryFunctionCall(rPortObject.getLibraries(), false), false);
            } else if (port instanceof BufferedDataTable) {
                exec.setMessage("Exporting data to R");
                // write all input data to the R session
                monitoredAssign("knime.in", (BufferedDataTable)port, exec.createSubProgress(0.5), batchSize, rType,
                    sendRowNames);
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
                    flowVars.add(new FlowVariable((String)rList.names.get(c), rexp.asInteger()));
                } else if (rexp.isNumeric()) {
                    flowVars.add(new FlowVariable((String)rList.names.get(c), rexp.asDouble()));
                } else if (rexp.isString()) {
                    flowVars.add(new FlowVariable((String)rList.names.get(c), rexp.asString()));
                }
            }
        } catch (final REXPMismatchException e) {
            throw new RException("Error importing flow variables from \"" + variableName + "\"", e);
        } catch (final REngineException e) {
            // the variable name was not found.
        }
        return flowVars;
    }

    /**
     * Create an REXPLogical for a BooleanValue or {@link REXPLogical#isNA()}.
     */
    private static byte exportBooleanValue(final DataCell cell) {
        if (cell.isMissing()) {
            return REXPLogical.NA;
        }
        return ((BooleanValue)cell).getBooleanValue() ? REXPLogical.TRUE : REXPLogical.FALSE;
    }

    /**
     * Create an int for a IntValue or create a {@link REXPInteger#NA}.
     */
    private static int exportIntValue(final DataCell cell) {
        if (cell.isMissing()) {
            return REXPInteger.NA;
        }
        return ((IntValue)cell).getIntValue();
    }

    /**
     * Create a double for a DoubleValue or {@link REXPDouble#NA}.
     */
    private static double exportDoubleValue(final DataCell cell) {
        if (cell.isMissing()) {
            return REXPDouble.NA;
        }
        return ((DoubleValue)cell).getDoubleValue();
    }

    /**
     * Create a String for a StringValue or null.
     */
    private static String exportStringValue(final DataCell cell) {
        if (!cell.isMissing()) {
            return cell.toString();
        } else {
            return null;
        }
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
                LOGGER.debug("Using experimental support for receiving data as \"data.table\".");
            } else if (type.equals("matrix") || type.equals("list")) {
                eval(varName + "<-data.frame(" + varName + ")", false);
            } else if (!type.equals("data.frame")) {
                throw new RException(
                    "CODING PROBLEM\timportBufferedDataTable(): Supporting only 'data.frame', "
                        + "'data.table', 'matrix' and 'list' for type of \"" + varName + "\" (was '" + type + "').",
                    null);
            }
        } catch (final REXPMismatchException e) {
            throw new RException("Type of " + varName + " could not be parsed as string.", e);
        }

        final ThreadPool threadPool = ThreadPool.currentPool();

        try {
            // Get column names
            final String[] columnNames = eval("colnames(" + varName + ")", true).asStrings();
            final int numColumns = columnNames.length;

            // Get row count (and row names if not automatic compact 1:n storage)
            final REXP numRowsRexp = getREngine().eval(".row_names_info(" + varName + ")");
            final boolean compactRowNames = numRowsRexp.asInteger() < 0;
            if (!compactRowNames) {
                eval("knime.out.row.names<-attr(" + varName + ",\"row.names\")", false);
            }
            final int numRows = Math.abs(numRowsRexp.asInteger());
            int transferredRows = 0;

            final DataColumnSpec[] colSpecs = new DataColumnSpec[numColumns];
            BufferedDataContainer cont = null;

            final DataCell[][] columns = new DataCell[numColumns][];
            @SuppressWarnings("unchecked")
            final Future<Void>[] futures = new Future[numColumns];
            Future<Void> addRowsFuture = null;

            while (transferredRows < numRows) {
                // this is NOT the chunk size value as per config dialog as receiving data happens
                // in chunks _and_ on columns
                final int remRows = numRows - transferredRows;
                int rowsThisBatch = Math.min(50000, remRows);
                if ((remRows - rowsThisBatch) < 10000) {
                    // avoid final chunk being smaller than 10k rows
                    rowsThisBatch = remRows;
                }

                // Expression for range of rows to transfer e.g.: 1:10000
                final String rowRangeExpr = (transferredRows + 1) + ":" + (transferredRows + rowsThisBatch);

                for (int i = 0; i < numColumns; ++i) {
                    exec.checkCanceled();
                    exec.setProgress((transferredRows / (double)numRows)
                        + ((rowsThisBatch / (double)numRows) * (i / (double)numColumns)));

                    final int rIndex = i + 1; // R starts indices at 1
                    final String expr = varName + ((isDataTable) ? "[[" + rIndex + "]][" + rowRangeExpr + "]"
                        : "[" + rowRangeExpr + "," + rIndex + "]");
                    final REXP column = eval(expr, true);

                    if (transferredRows == 0) {
                        // Create column spec for this column
                       colSpecs[i] = new DataColumnSpecCreator(columnNames[i], getColType(column)).createSpec();
                    }

                    if (addRowsFuture != null) {
                        try {
                            addRowsFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException("Error while adding rows to table.", e);
                        }
                    }

                    // Convert values
                    if ((columns[i] == null) || (columns[i].length < rowsThisBatch)) {
                        // Only reallocate the DataCell buffer if insufficient size.
                        columns[i] = new DataCell[rowsThisBatch];
                    }

                    if (column.isNull()) {
                        Arrays.fill(columns[i], DataType.getMissingCell());
                    } else {
                        if (column.isList()) {
                            final DataCell[] list = columns[i];
                            int row = 0;
                            for (final Object o : column.asList()) {
                                final REXP rexp = (REXP)o;
                                if (rexp.isNull()) {
                                    list[row] = DataType.getMissingCell();
                                } else {
                                    if (rexp.isVector()) {
                                        final REXPVector colValue = (REXPVector)rexp;
                                        final DataCell[] listCells = new DataCell[colValue.length()];
                                        importCells(colValue, listCells, nonNumbersAsMissing);
                                        list[row] = CollectionCellFactory.createListCell(Arrays.asList(listCells));
                                    } else {
                                        LOGGER.warn(
                                            "Expected Vector type for list cell. Inserting missing cell instead.");
                                        list[row] = DataType.getMissingCell();
                                    }
                                }
                                ++row;
                            }
                        } else {
                            final DataCell[] columnCells = columns[i];
                            final Callable<Void> callable = ThreadUtils.callableWithContext(() -> {
                                importCells(column, columnCells, nonNumbersAsMissing);
                                return null;
                            });
                            futures[i] =
                                threadPool != null ? threadPool.enqueue(callable) : R_THREAD_POOL.submit(callable);
                        }
                    }
                }

                if (cont == null) {
                    // create container and outspec for the first batch of rows
                    cont = exec.createDataContainer(new DataTableSpec(colSpecs));
                }

                Stream.of(futures).filter(o -> o != null).forEach(f -> {
                    try {
                        f.get();
                    } catch (final Throwable e) {
                        throw new RuntimeException("Error during conversion of R values to KNIME types.", e);
                    }
                });

                final REXP rRowIds =
                    (compactRowNames) ? null : getREngine().eval("knime.out.row.names[" + rowRangeExpr + "]");

                final DataCell[] curRow = new DataCell[numColumns];

                final BufferedDataContainer finalCont = cont;
                final int finalTransferredRows = transferredRows;
                final int finalRowsThisBatch = rowsThisBatch;
                // Should never happen, only happens if Rserve returns less bytes than expected. Maybe a version issue?
                CheckUtils.checkState(compactRowNames || (rRowIds != null), "Received an invalid packet from Rserve.");
                final Callable<Void> addRowsCallable = ThreadUtils.callableWithContext(() -> {
                    @SuppressWarnings("null")
                    final String[] rowIds = compactRowNames ? null : rRowIds.asStrings();
                    for (int i = 0; i < finalRowsThisBatch; ++i) {
                        @SuppressWarnings("null")
                        final RowKey rowKey = compactRowNames ? new RowKey(Long.toString(1 + i + finalTransferredRows))
                            : new RowKey(rowIds[i]);
                        for (int col = 0; col < columns.length; ++col) {
                            curRow[col] = columns[col][i];
                        }
                        finalCont.addRowToTable(new DefaultRow(rowKey, curRow));
                    }
                    return null;
                });
                addRowsFuture =
                    threadPool != null ? threadPool.enqueue(addRowsCallable) : R_THREAD_POOL.submit(addRowsCallable);

                transferredRows += rowsThisBatch;
            }

            if (addRowsFuture != null) {
                try {
                    addRowsFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Error while adding rows to table.", e);
                }
            }

            // can only happen if knime.out is an empty data.frame/table
            if (cont == null) {
                assert transferredRows == 0 : "No output container was initialized, although knime.out contained data";
                // create container and outspec for the first batch of rows
                final RList data = getREngine().get(varName, null, true).asList();
                cont = exec.createDataContainer(createSpecFromEmptyDataFrame(columnNames, data));
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
     * Creates the proper data table spec from an empty input data.frame/table.
     *
     * @param cNames the column names
     * @param data the empty data.frame/table
     * @return the output data table spec
     */
    private static DataTableSpec createSpecFromEmptyDataFrame(final String[] cNames, final RList data) {
        assert data.size() == cNames.length : "The number of column names differs from the actual number of columns";
        final DataColumnSpec[] cSpecs = new DataColumnSpec[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            cSpecs[i] = new DataColumnSpecCreator(cNames[i], getColType(data.at(i))).createSpec();
        }
        return new DataTableSpec(cSpecs);
    }

    /**
     * Returns the proper type for the given column.
     *
     * @param c the column whose type has to be determined
     * @return the type of the column
     */
    private static DataType getColType(final REXP c) {
        if (!c.isList()) {
            return importDataType(c);
        }
        return DataType.getType(ListCell.class, DataType.getType(DataCell.class));
    }

    /**
     * Import all cells from a R expression and put them into <code>column</code>.
     *
     * @param rexp Source of values
     * @param column ArrayList to store the created DataCells into.
     * @param nonNumbersAsMissing Convert NaN and Infinity to {@link MissingCell}.
     * @throws REXPMismatchException
     */
    private static final void importCells(final REXP rexp, final DataCell[] column, final boolean nonNumbersAsMissing)
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
            final double[] doubles = rexp.asDoubles();
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

    /**
     * Get cell type as which a REXP would be imported.
     */
    private static DataType importDataType(final REXP column) {
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
        } catch (final InterruptedException e) {
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
        } catch (final InterruptedException e) {
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
        } catch (final REXPMismatchException e) {
            LOGGER.error("Rengine error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /** Map of type to a R expression which creates a column vector for that type */
    private static final Map<Class<? extends DataValue>, String> DATA_TYPE_TO_R_CONSTRUCTOR;

    private static final AtomicInteger R_THREAD_POOL_INDEX = new AtomicInteger();

    private static final ExecutorService R_THREAD_POOL = new ThreadPoolExecutor(0,
        Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
        r -> new Thread(r, "R-DataExchange-" + R_THREAD_POOL_INDEX.getAndIncrement()));

    static {
        final Map<Class<? extends DataValue>, String> tmp = new HashMap<>();

        tmp.put(IntValue.class, "integer(rowCount)");
        tmp.put(BooleanValue.class, "logical(rowCount)");
        tmp.put(DoubleValue.class, "double(rowCount)");
        /* For character default value is "", which would create a "" level. */
        tmp.put(StringValue.class, "factor(as.character(rep(NA, rowCount)))");
        tmp.put(CollectionDataValue.class, "vector(mode='list', length=rowCount)");

        DATA_TYPE_TO_R_CONSTRUCTOR = Collections.unmodifiableMap(tmp);
    }

    /* This class ties together all variables concerning a single batch */
    private static final class Batch {
        final int m_size;

        /** Size of this batch */
        int m_index = 0;

        /** Current row index for writing to */

        final RList m_rBatch; // List containing rColumns

        final REXPString m_rRowNames;

        final REXPGenericVector m_rVector;

        final Map<Integer, LinkedHashMap<String, Integer>> m_factorLevels = new HashMap<>();

        final Map<Integer, int[]> m_factorIndices = new HashMap<>();

        /**
         * @param numRows Number of rows for this batch.
         * @param columnCount Number of columns.
         */
        public Batch(final int numRows, final int columnCount) {
            m_size = numRows;

            m_rRowNames = new REXPString(new String[numRows]);
            m_rBatch = new RList(columnCount + 1, false);
            m_rVector = new REXPGenericVector(m_rBatch);
        }

        /**
         * Create REXPFactor instance from levels (String[]) and indices (int[]) for every factor column.
         */
        public void postProcessFactorColumns() {
            for (final Map.Entry<Integer, LinkedHashMap<String, Integer>> entry : m_factorLevels.entrySet()) {
                final int columnIndex = entry.getKey();
                final LinkedHashMap<String, Integer> factorLevels = entry.getValue();

                final String[] levels = factorLevels.keySet().stream().toArray(n -> new String[n]);
                final int[] indices = m_factorIndices.get(columnIndex);

                /* Create a REXP factor for this batch without copying the indices and levels */
                final REXPFactor factor = new REXPFactor(new RFactor(indices, levels, false, 1));
                m_rBatch.set(columnIndex, factor);
            }
        }
    }

    @Override
    public void monitoredAssign(final String name, final BufferedDataTable table, final ExecutionMonitor exec,
        final int batchSize, final String rType, final boolean sendRowNames)
        throws RException, CanceledExecutionException {

        final int rowCount = KnowsRowCountTable.checkRowCount(table.size());
        final int columnCount = table.getDataTableSpec().getNumColumns();

        if (columnCount == 0) {
            // Special case of empty table input. R doesn't seem to have "only row names" case, so we
            // just handle this as in 2.12, where this resulted in a empty data.frame without rows or columns.
            eval(name + "<-data.frame()", false);
            exec.setProgress(1.0);
            return;
        }

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
        final StringBuilder cleanupScript =
            new StringBuilder("rm(knime.row.names,knime.col.names,bt,i,rowCount,colCount,cols");

        // script for combining the individual columns into a data.frame (or data.table)
        final boolean useDataTable = "data.table".equals(rType);
        if (useDataTable) {
            final REXP ret = eval("require('data.table')", true);
            try {
                if (!Boolean.parseBoolean(ret.asString())) {
                    throw new RuntimeException(
                        "Selected data.table as type for \"" + name + "\", but package could not be found.");
                }
            } catch (final REXPMismatchException e) {
                throw new IllegalStateException("\"find.package\" doesn't return string anymore.", e);
            }
            LOGGER.debug("Using experimental support for sending data as \"data.table\".");
        }

        // Variables concerning a single batch
        final Batch batch = new Batch(Math.min(batchSize, rowCount), columnCount);

        // Get type of each column and generate R code which creates an appropriate column
        int columnIndex = 0;
        for (final DataColumnSpec columnSpec : table.getDataTableSpec()) {
            final String columnVar = "cols[[" + (columnIndex + 1) + "]]";

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
            if (dataValueClass == CollectionDataValue.class) {
                eval(columnVar + "<-I(" + constructor + ")", false); // Allocate vector for column, e.g.: c10 <- double(12345)
            } else {
                eval(columnVar + "<-" + constructor, false); // Allocate vector for column, e.g.: c10 <- double(12345)
            }

            // Prepare KNIME side batch for this column
            if (dataValueClass == CollectionDataValue.class) {
                final RList col = new RList(batch.m_size, false);
                IntStream.range(0, batch.m_size).forEach(i -> col.add(null));
                batch.m_rBatch.add(new REXPGenericVector(col));
            } else if (dataValueClass == BooleanValue.class) {
                batch.m_rBatch.add(new REXPLogical(new byte[batch.m_size]));
            } else if (dataValueClass == IntValue.class) {
                batch.m_rBatch.add(new REXPInteger(new int[batch.m_size]));
            } else if (dataValueClass == DoubleValue.class) {
                batch.m_rBatch.add(new REXPDouble(new double[batch.m_size]));
            } else if (dataValueClass == StringValue.class) {
                /* Create index array and level map for factor columns */
                final int[] indices = new int[batch.m_size]; /* Will be reused every batch */
                final String[] levels = new String[0]; /* No levels yet and not reused */

                batch.m_factorIndices.put(columnIndex, indices);
                batch.m_factorLevels.put(columnIndex, new LinkedHashMap<String, Integer>());

                batch.m_rBatch.add(new REXPFactor(new RFactor(indices, levels, false, 1)));
            } else {
                batch.m_rBatch.add(new REXPString(new String[batch.m_size]));
            }

            columnIndex++;

            exec.checkCanceled(); // Useful for many many rows
        }

        if (sendRowNames) {
            // Allocate vector for row names on R side
            eval("knime.row.names<-character(rowCount)", false);
            cleanupScript.append(",knime.row.names");

            // And on KNIME side
            batch.m_rBatch.add(batch.m_rRowNames);
        }

        exec.setMessage("Sending column names.");
        // transfer column names to Rserve
        monitoredAssign("knime.col.names", new REXPString(table.getDataTableSpec().getColumnNames()), exec);

        if (useDataTable) {
            // Create data.table now, we will use set(table, column, row, newData) to set values in the table directly
            try {
                monitoredEval("library(data.table);" + name + "<-as.data.table(cols, check.names=FALSE);names(" + name
                    + ")<-knime.col.names", exec, false);
            } catch (final InterruptedException e) {
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
            if ((System.currentTimeMillis() - timeSinceUpdate) > 1000) {
                LOGGER.debugWithFormat("Rows per second: %d", rowsSinceUpdate);
                rowsSinceUpdate = 0;
                timeSinceUpdate = System.currentTimeMillis();
            } else {
                ++rowsSinceUpdate;
            }

            if (sendRowNames) {
                batch.m_rRowNames.asStrings()[batch.m_index] = row.getKey().getString();
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
                            final CollectionDataValue collValue = (CollectionDataValue)cell;
                            final DataType elementType = cell.getType().getCollectionElementType();
                            if (elementType.isCompatible(BooleanValue.class)) {
                                final byte[] elementValue = new byte[collValue.size()];
                                int i = 0;
                                for (final DataCell e : collValue) {
                                    elementValue[i] = exportBooleanValue(e);
                                    ++i;
                                }
                                value = new REXPLogical(elementValue);

                            } else if (elementType.isCompatible(IntValue.class)) {
                                final int[] elementValue =
                                    collValue.stream().mapToInt(RController::exportIntValue).toArray();
                                value = new REXPInteger(elementValue);
                            } else if (elementType.isCompatible(DoubleValue.class)) {
                                final double[] elementValue =
                                    collValue.stream().mapToDouble(RController::exportDoubleValue).toArray();
                                value = new REXPDouble(elementValue);
                            } else {
                                final String[] elementValue =
                                    collValue.stream().map(RController::exportStringValue).toArray(String[]::new);
                                value = new REXPString(elementValue);
                            }
                        }
                        ((REXP)batch.m_rBatch.get(c)).asList().set(batch.m_index, value);
                    } else {
                        final REXP curREXP = (REXP)batch.m_rBatch.get(c);
                        if (type.equals(BooleanValue.class)) {
                            curREXP.asBytes()[batch.m_index] = exportBooleanValue(cell);
                        } else if (type.equals(IntValue.class)) {
                            curREXP.asIntegers()[batch.m_index] = exportIntValue(cell);
                        } else if (type.equals(DoubleValue.class)) {
                            curREXP.asDoubles()[batch.m_index] = exportDoubleValue(cell);
                        } else if (type.equals(StringValue.class)) {
                            final LinkedHashMap<String, Integer> factors = batch.m_factorLevels.get(c);
                            int index = REXPInteger.NA;
                            if (!cell.isMissing()) {
                                /* Get factor index for the value */
                                final String value = ((StringValue)cell).getStringValue();
                                final Integer idx = factors.get(value);
                                if (idx == null) {
                                    /* First occurance of this string value, add it to map of factor levels */
                                    index = factors.size() + 1; // R indices are base 1
                                    factors.put(value, index);
                                } else {
                                    index = idx;
                                }
                            }
                            batch.m_factorIndices.get(c)[batch.m_index] = index;
                        } else {
                            curREXP.asStrings()[batch.m_index] = exportStringValue(cell);
                        }
                    }
                } catch (final REXPMismatchException e) {
                    // Will never happen, the REXPs types are added according to column types.
                    throw new IllegalStateException(e);
                }
                ++c;
            }

            ++batch.m_index;
            if ((batch.m_index == batch.m_size) || ((batch.m_index + rowIndex) == rowCount)) {
                // Batch full or end of table

                batch.postProcessFactorColumns(); /* Create REXPFactor from int[] and level hash map */

                assign("bt", batch.m_rVector);

                final long start = rowIndex + 1;
                final long end = rowIndex + batch.m_index;

                /* Assign data from chunk/batch to final table column-wise. If it's a factor column, we need to grow the level set, otherwise values will end up as NA */
                if (useDataTable) {
                    eval("for(i in 1:colCount){if(is.factor(bt[[i]])){levels(" + name + "[[i]])<-levels(bt[[i]])};set("
                        + name + "," + start + ":" + end + ",i,bt[i])}", false);
                } else {
                    eval("for(i in 1:colCount){if(is.factor(bt[[i]])){levels(cols[[i]])<-levels(bt[[i]])};cols[[i]]["
                        + start + ":" + end + "]<-bt[[i]][1:" + batch.m_index + "]}", false);
                }

                if (sendRowNames) {
                    eval("knime.row.names[" + start + ":" + end + "]<-bt[[colCount+1]][1:" + batch.m_index + "]",
                        false);
                }

                // Not relevant if batch.index+rowIndex == rowCount
                rowIndex += batch.m_size;
                batch.m_index = 0;

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
                    monitoredEval(name + "<-as.data.frame(cols,row.names=knime.row.names,check.names=FALSE);names("
                        + name + ")<-knime.col.names", exec, false);
                } else {
                    monitoredEval(name + "<-as.data.frame(cols,check.names=FALSE);names(" + name + ")<-knime.col.names",
                        exec, false);
                }
            }
        } catch (final InterruptedException e) {
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
        } catch (final InterruptedException e) {
            throw new RException("Interrupted while saving R workspace.", e);
        }
    }

    /**
     * A function call that loads all libraries in the argument but checking if they are not loaded yet.
     *
     * @param listOfLibraries List of libraries from upstream node (e.g. randomForest, tree, ...)
     * @param suppressMessages if true the library call is wrapped so that no output is printed
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
     * Evaluation of R code with a monitor in a separate thread to cancel the code execution in case the execution of
     * the node is cancelled.
     */
    private final class MonitoredEval {

        private final int m_interval = 200;

        private final ExecutionMonitor m_exec;

        /**
         * Constructor
         *
         * @param exec for tracking progress and checking cancelled state.
         */
        public MonitoredEval(final ExecutionMonitor exec) {
            m_exec = exec;
        }

        /*
         * Run the Callable in a thread and make sure to cancel it, in case
         * execution is cancelled.
         */
        private REXP monitor(final Callable<REXP> task)
            throws InterruptedException, RException, CanceledExecutionException {
            final FutureTask<REXP> runningTask = new FutureTask<>(task);
            final Thread t = (m_useNodeContext) ? ThreadUtils.threadWithContext(runningTask, "R-Evaluation")
                : new Thread(runningTask, "R-Evaluation");
            t.start();

            try {
                while (!runningTask.isDone()) {
                    Thread.sleep(m_interval);
                    m_exec.checkCanceled();
                }
                // AP-11567: We need to wait for the other thread to die. If the thread lives until the
                // finally block we would terminate the Rserve instance without a reason.
                t.join();

                return runningTask.get();
            } catch (final ExecutionException e) {
                if (e.getCause() instanceof RException) {
                    throw (RException)e.getCause();
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
        public REXP run(final String cmd, final boolean resolve) throws REngineException, REXPMismatchException,
            RException, CanceledExecutionException, InterruptedException {
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
    }
}