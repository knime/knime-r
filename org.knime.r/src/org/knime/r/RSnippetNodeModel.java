/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the R integration plugin for KNIME.
 *
 * The R integration plugin is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.org.
 * ---------------------------------------------------------------------
 *
 * History
 *   17.09.2007 (thiel): created
 */
package org.knime.r;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.interactive.InteractiveNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.util.FileUtil;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.ext.r.node.local.port.RPortObjectSpec;
import org.knime.r.preferences.RPreferenceInitializer;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * The <code>RSnippetNodeModel</code> provides functionality to create
 * a R script with user defined R code and run it.
 *
 * @author Heiko Hofer
 */
public class RSnippetNodeModel extends ExtToolOutputNodeModel 
		implements InteractiveNode<RSnippetViewContent>, BufferedDataTableHolder {
    private RSnippet m_snippet;
	private BufferedDataTable m_data;
	private DataTableSpec m_configSpec;
	private RSnippetNodeConfig m_config;
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
        "R Snippet");
    
    /**
     * The temp directory used as a working directory for R
     */
    static final String TEMP_PATH = KNIMEConstants.getKNIMETempDir().replace('\\', '/');    
    
    /**
     * Creates new instance of <code>RSnippetNodeModel</code> with one
     * data in and data one out port.
     * @param pref R preference provider
     */
    public RSnippetNodeModel(final RSnippetNodeConfig config) {
        super(config.getInPortTypes().toArray(new PortType[config.getInPortTypes().size()]), 
        		config.getOutPortTypes().toArray(new PortType[config.getOutPortTypes().size()]));;
        m_snippet = new RSnippet();
        m_snippet.attachLogger(LOGGER);
        m_config = config;
    }  

	/**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        for (PortObjectSpec inSpec : inSpecs) {
        	if (inSpec instanceof DataTableSpec) {
        		m_configSpec = (DataTableSpec)inSpec;
        	}
        }
        
        FlowVariableRepository flowVarRepository =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        
        ValueReport<DataTableSpec> report = m_snippet.configure(m_configSpec,
                flowVarRepository);

        if (report.hasWarnings()) {
            setWarningMessage(joinString(report.getWarnings(), "\n"));
        }
        if (report.hasErrors()) {
            throw new InvalidSettingsException(
                    joinString(report.getErrors(), "\n"));
        }

        for (FlowVariable flowVar : flowVarRepository.getModified()) {
            if (flowVar.getType().equals(Type.INTEGER)) {
                pushFlowVariableInt(flowVar.getName(), flowVar.getIntValue());
            } else if (flowVar.getType().equals(Type.DOUBLE)) {
                pushFlowVariableDouble(flowVar.getName(),
                        flowVar.getDoubleValue());
            } else {
                pushFlowVariableString(flowVar.getName(),
                        flowVar.getStringValue());
            }
        }
        
        Collection<PortObjectSpec> outSpec = new ArrayList<PortObjectSpec>(4);
        for (PortType portType : m_config.getOutPortTypes()) {
        	if (portType.equals(BufferedDataTable.TYPE)) {
        		outSpec.add(null);
        	} else if (portType.equals(RPortObject.TYPE)) {
        		outSpec.add(RPortObjectSpec.INSTANCE);
        	} 
        }
        return outSpec.toArray(new PortObjectSpec[outSpec.size()]);
    }

    /**
     * Concatenate strings with delimiter.
     * @param strings the string
     * @param delim the delimiter
     * @return concatenated string
     */
    private String joinString(final String[] strings, final String delim) {
    	if (null == strings || strings.length == 0) {
    		return "";
    	}
		StringBuilder b = new StringBuilder();
		b.append(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			b.append(delim);
			b.append(strings);
		}
		return b.toString();
	}


	/**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
    	return executeInternal(m_snippet.getSettings(), inData, exec);
    }
    

	private PortObject[] executeInternal(final RSnippetSettings settings,
			final PortObject[] inData, final ExecutionContext exec) {
        m_snippet.getSettings().loadSettings(settings);
        for (PortObject in : inData) {
        	if (in instanceof BufferedDataTable) {
        		m_data = (BufferedDataTable)in;
        	}
        }        

        FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());
        ValueReport<PortObject[]> out = executeSnippet(inData, flowVarRepo, exec);
        
        if (out.hasWarnings()) {
        	setWarningMessage(joinString(out.getWarnings(), "\n"));
        }            
        if (out.hasErrors()) {
        	throw new RuntimeException(joinString(out.getErrors(), "\n"));
        }
    
        pushFlowVariables(flowVarRepo);
        
        return out.getValue();
	}

	@Override
	public PortObject[] reExecute(final RSnippetViewContent content,
			final PortObject[] data, final ExecutionContext exec)
			throws CanceledExecutionException {
		return executeInternal(content.getSettings(), data, exec);
	}
	
	

	@Override
	public RSnippetViewContent createViewContent() {
		RSnippetSettings settings = new RSnippetSettings();
		settings.loadSettings(m_snippet.getSettings());
		return new RSnippetViewContent(settings);
	}
    
    private ValueReport<PortObject[]> executeSnippet(final PortObject[] inData,
			final FlowVariableRepository flowVarRepo, final ExecutionContext exec) {
    	List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        
        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<String>());
        setFailedExternalOutput(new LinkedList<String>());
        
        File tempWorkspaceFile = null;
        
    	try {
    		tempWorkspaceFile = exportData(flowVarRepo, exec);
    		runRScript(tempWorkspaceFile, exec);    	
    		

            Collection<PortObject> outPorts = new ArrayList<PortObject>(4);
            for (PortType portType : m_config.getOutPortTypes()) {
            	if (portType.equals(BufferedDataTable.TYPE)) {
            		outPorts.add(importData(tempWorkspaceFile, exec));
            	} else if (portType.equals(RPortObject.TYPE)) {
            		outPorts.add(new RPortObject(tempWorkspaceFile));
            	} 
            }
            // TODO tempWorkspaceFile is load twice in case that there is a data table outport
            importFlowVariables(tempWorkspaceFile, flowVarRepo, exec);
	        
			return new ValueReport<PortObject[]>(outPorts.toArray(new PortObject[outPorts.size()]), errors, warnings);
			
		} catch (Exception e) {
			errors.add(e.getMessage());
			// TODO: Remove later
			e.printStackTrace();
			LOGGER.error(e);
			return new ValueReport<PortObject[]>(null, errors, warnings);
		}    	
	}


	private void runRScript(final File tempWorkspaceFile, final ExecutionContext exec) throws Exception {
    	
    	String rScript = buildRScript(tempWorkspaceFile);

        // tmp files
        File rCommandFile = null;
        File rOutFile = null;

		try {
			rCommandFile = writeRcommandFile(rScript);
			
	        rOutFile = new File(rCommandFile.getAbsolutePath() + ".Rout");
	    	
	        // create shell command
	        StringBuilder shellCmd = new StringBuilder();
	
	        final String rBinaryFile = getRBinaryPathAndArguments();
	        shellCmd.append(rBinaryFile);
	        shellCmd.append(" ");
	        shellCmd.append(rCommandFile.getName());
	        shellCmd.append(" ");
	        shellCmd.append(rOutFile.getName());
	
	        executeExernal(shellCmd.toString(), rCommandFile, rOutFile, exec);			
		} catch (Exception e) {
			throw e;
		} finally {
			// delete all temp files
			deleteFile(rCommandFile);
			deleteFile(rOutFile);
		}
	}
    
    private void executeExernal(final String shcmd, final File rCommandFile, final File rOutFile, final ExecutionContext exec) throws Exception {
        // execute shell command
        LOGGER.debug("Shell command: \n" + shcmd);

        CommandExecution cmdExec = new CommandExecution(shcmd);
        cmdExec.addObserver(this);
        cmdExec.setExecutionDir(rCommandFile.getParentFile());
        int exitVal = cmdExec.execute(exec);

        setExternalErrorOutput(new LinkedList<String>(cmdExec.getStdErr()));
        setExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));

        String rErr = "";

        if (exitVal != 0) {
            // before we return, we save the output in the failing list
            synchronized (cmdExec) {
                setFailedExternalOutput(new LinkedList<String>(cmdExec.getStdOutput()));
            }
        }
        synchronized (cmdExec) {

            // save error description of the Rout file to the ErrorOut
            LinkedList<String> list = new LinkedList<String>(cmdExec.getStdErr());

            list.add("#############################################");
            list.add("#");
            list.add("# Content of .Rout file: ");
            list.add("#");
            list.add("#############################################");
            list.add(" ");
            BufferedReader bfr = new BufferedReader(new FileReader(rOutFile));
            String line;
            while ((line = bfr.readLine()) != null) {
                list.add(line);
            }
            bfr.close();

            // use row before last as R error.
            int index = list.size() - 2;
            if (index >= 0) {
                rErr = list.get(index);
            }

            if (exitVal != 0) {
                setFailedExternalErrorOutput(list);
                LOGGER.debug("Execution of R Script failed with exit code: " + exitVal);
                throw new IllegalStateException("Execution of R script failed: " + rErr);
            } else {
                setExternalOutput(list);
            }
        }
	}

	/**
     * Deletes the specified file. If the file is a directory the directory
     * itself as well as its files and sub-directories are deleted.
     *
     * @param file The file to delete.
     * @return <code>true</code> if the file could be deleted, otherwise
     * <code>false</code>.
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
                    LOGGER.debug(file.getAbsoluteFile()
                            + " could not be deleted !");
                }
            }
        }
        return del;
    }    
    
    /**
     * Path to R binary together with the R arguments <code>CMD BATCH</code> and
     * additional options.
     * @return R binary path and arguments
     */
    protected final String getRBinaryPathAndArguments() {
        String argR = retRArguments();
        if (!argR.isEmpty()) {
            argR = " " + argR;
        }
        return getRBinaryPath() + " CMD BATCH" + argR;
    }

    private String retRArguments() {
		return "--vanilla";
	}

	/**
     * Path to R binary.
     * @return R binary path
     */
    protected final String getRBinaryPath() {
    	return RPreferenceInitializer.getRProvider().getRBinPath();
    }    

    /**
     * Writes the given string into a file and returns it.
     *
     * @param cmd The string to write into a file.
     * @return The file containing the given string.
     * @throws IOException If string could not be written to a file.
     */
    static File writeRcommandFile(final String cmd) throws IOException {
        File tempCommandFile = File.createTempFile("R-inDataTempFile-", ".r", new File(TEMP_PATH));
        tempCommandFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempCommandFile);
        fw.write(cmd);
        fw.close();
        return tempCommandFile;
    }
    
	private String buildRScript(final File tempWorkspaceFile) throws BadLocationException {
    	StringBuilder rScript = new StringBuilder();
		// set working directory
		rScript.append("setwd(\"");
		rScript.append(TEMP_PATH);
		rScript.append("\");\n");
		// load workspace
		rScript.append("load(\"");
		rScript.append(tempWorkspaceFile.getAbsolutePath().replace('\\', '/'));
		rScript.append("\");\n");
		
		// add node specific prefix
		rScript.append(m_config.getScriptPrefix());
		// user defined script
		String userScript = m_snippet.getDocument().getText(0, m_snippet.getDocument().getLength());
		rScript.append(userScript.trim());
		rScript.append("\n");
		// append node specific suffix
		rScript.append(m_config.getScriptSuffix());
		// save workspace to temporary file
		rScript.append("save.image(\"");
		rScript.append(tempWorkspaceFile.getAbsolutePath().replace('\\', '/'));
		rScript.append("\");\n");
		
		return rScript.toString();
	}

	private BufferedDataTable importData(final File tempWorkspaceFile, final ExecutionContext exec) throws REngineException, REXPMismatchException {
    	RController r = RController.getDefault();
    	// TODO: lock controller
		r.clearWorkspace();
		
		r.loadWorkspace(tempWorkspaceFile);
		
    	BufferedDataTable out = r.importBufferedDataTable("knime.out", exec);
		
    	// TODO: unlock controller
    	return out;
	}
	


	private void importFlowVariables(final File tempWorkspaceFile, final FlowVariableRepository flowVarRepo,
			final ExecutionContext exec) throws REngineException, REXPMismatchException {
    	RController r = RController.getDefault();
    	// TODO: lock controller
		r.clearWorkspace();
		
		r.loadWorkspace(tempWorkspaceFile);
		
    	Collection<FlowVariable> flowVars = r.importFlowVariables("knime.flow.out", exec);
    	for (FlowVariable flowVar : flowVars) {
    		flowVarRepo.put(flowVar);
    	}
		
    	// TODO: unlock controller
    	
		
	}	

	private File exportData(final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws REngineException, REXPMismatchException, IOException {
    	RController r = RController.getDefault();
    	// TODO: lock controller
		r.clearWorkspace();
		if (m_data != null) {
			r.exportDataTable(m_data, "knime.in", exec);
		}
		r.exportFlowVariables(flowVarRepo.getInFlowVariables(), "knime.flow.in", exec);
		
		File tempFolder = new File(TEMP_PATH);
		// save workspace to temporary file
		File tempWorkspaceFile = File.createTempFile("R-workspace", ".RData", tempFolder);
		r.saveWorkspace(tempWorkspaceFile);
		
		// TODO: unlock controller
		
		return tempWorkspaceFile;
	}
	

	/**
     * Push changed flow variables.
     */
	private void pushFlowVariables(final FlowVariableRepository flowVarRepo) {
        for (FlowVariable var : flowVarRepo.getModified()) {
            Type type = var.getType();
            if (type.equals(Type.INTEGER)) {
                pushFlowVariableInt(var.getName(), var.getIntValue());
            } else if (type.equals(Type.DOUBLE)) {
                pushFlowVariableDouble(var.getName(), var.getDoubleValue());
            } else { // case: type.equals(Type.STRING)
                pushFlowVariableString(var.getName(), var.getStringValue());
            }
        }
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_snippet.getSettings().saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RSnippetSettings s = new RSnippetSettings();
        s.loadSettings(settings);
        // TODO: Check settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_snippet.getSettings().loadSettings(settings);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals, nothing to reset.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals.
    }

	public DataTableSpec getInputSpec() {
		return m_data != null ? m_data.getDataTableSpec() : m_configSpec;
	}
	
	public RSnippetSettings getSettings() {
		return m_snippet.getSettings();
	}
	
	protected RSnippetNodeConfig getRSnippetNodeConfig() {
		return m_config;
	}
	
	public BufferedDataTable getInputData() {
		return m_data;
	}

	public void loadSettings(final RSnippetSettings settings) {
		m_snippet.getSettings().loadSettings(settings);
	}

	@Override
	public BufferedDataTable[] getInternalTables() {
		return m_data != null ? new BufferedDataTable[] {m_data} : new BufferedDataTable[0];
	}

	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		if (tables.length > 0) {
			m_data = tables[0];
		}
	}
}
