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
import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.knime.base.node.util.exttool.CommandExecution;
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
import org.knime.r.preferences.RPreferenceInitializer;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * The <code>RSnippetNodeModel</code> provides functionality to create
 * a R script with user defined R code and run it.
 *
 * @author Heiko Hofer
 */
public class RSnippetNodeModel extends ExtToolOutputNodeModel {
    private final RSnippet m_snippet;
	private final RSnippetNodeConfig m_config;
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
        "R Snippet");
    
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
        m_snippet.getSettings().setScript(config.getDefaultScript());
        m_config = config;
    }  

	/**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
    	DataTableSpec tableSpec = null;
        for (PortObjectSpec inSpec : inSpecs) {
        	if (inSpec instanceof DataTableSpec) {
        		tableSpec = (DataTableSpec)inSpec;
        	}
        }
        
        FlowVariableRepository flowVarRepository =
            new FlowVariableRepository(getAvailableInputFlowVariables());
        
        ValueReport<DataTableSpec> report = m_snippet.configure(tableSpec, flowVarRepository);

        if (report.hasWarnings()) {
            setWarningMessage(ValueReport.joinString(report.getWarnings(), "\n"));
        }
        if (report.hasErrors()) {
            throw new InvalidSettingsException(
            		ValueReport.joinString(report.getErrors(), "\n"));
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
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
    	return executeInternal(m_snippet.getSettings(), inData, exec);
    }
    

	private PortObject[] executeInternal(final RSnippetSettings settings,
			final PortObject[] inData, final ExecutionContext exec) throws CanceledExecutionException {
        m_snippet.getSettings().loadSettings(settings);

        FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());
        
        ValueReport<Boolean> isRAvailable = RController.getDefault().isRAvailable();
        if (!isRAvailable.getValue()) {
        	throw new RuntimeException(ValueReport.joinString(isRAvailable.getErrors(), "\n"));
        }
        boolean hasLock = RController.getDefault().tryAcquire();
        try {
			exec.setMessage("R is busy waiting...");
			while(!hasLock) {
				exec.checkCanceled();							
				hasLock = RController.getDefault().tryAcquire(500, TimeUnit.MILLISECONDS);
			
			}
			ValueReport<PortObject[]> out = executeSnippet(inData, flowVarRepo, exec);
			
	        if (out.hasWarnings()) {
	        	setWarningMessage(ValueReport.joinString(out.getWarnings(), "\n"));
	        }            
	        if (out.hasErrors()) {
	        	throw new RuntimeException(ValueReport.joinString(out.getErrors(), "\n"));
	        }
	        
	        pushFlowVariables(flowVarRepo);

	        return out.getValue();		        
        } catch (InterruptedException e) {
			// It is interrupted, ok
		} finally {
        	if (hasLock) {
        		RController.getDefault().release();
        	}
        	
        }
        return null;
	}
	
	

    
    private ValueReport<PortObject[]> executeSnippet(final PortObject[] inData,
			final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws CanceledExecutionException  {
    	List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        
        // blow away the output of any previous (failed) runs
        setFailedExternalErrorOutput(new LinkedList<String>());
        setFailedExternalOutput(new LinkedList<String>());
        
        File tempWorkspaceFile = null;
        
    	try {
    		// just to have a better progress report
    		double importTime = 0.0;
    		for (PortType portType : m_config.getOutPortTypes()) {
            	if (portType.equals(BufferedDataTable.TYPE)) {
            		importTime = 0.3;
            	}
            }
    		exec.setProgress(0.0);
    		exec.setMessage("Export data do to R");
    		tempWorkspaceFile = exportData(inData, flowVarRepo, exec.createSubExecutionContext(0.6 - importTime));
    		exec.setMessage("Run R");
    		runRScript(inData, tempWorkspaceFile, exec.createSubExecutionContext(1.0 - importTime));    	
    		exec.setProgress(1.0 - importTime);

    		exec.setMessage("Import data from R");
            RController r = RController.getDefault();
    		List<String> librariesInR = r.clearAndReadWorkspace(tempWorkspaceFile, exec);
            Collection<PortObject> outPorts = new ArrayList<PortObject>(4);
            for (PortType portType : m_config.getOutPortTypes()) {
            	if (portType.equals(BufferedDataTable.TYPE)) {
            		outPorts.add(importDataFromR(r, exec.createSubExecutionContext(1.0)));
            	} else if (portType.equals(RPortObject.TYPE)) {
            	    if (librariesInR == null) {
            	        librariesInR = importListOfLibrariesFromR(r);
            	    }
            		outPorts.add(new RPortObject(tempWorkspaceFile, librariesInR));
            	} 
            }
            exec.setMessage("Import flow variables from R");
            importFlowVariablesFromR(r, flowVarRepo, exec);
	        
			return new ValueReport<PortObject[]>(outPorts.toArray(new PortObject[outPorts.size()]), errors, warnings);
			
		} catch (Exception e) {
			if (e instanceof CanceledExecutionException) {
				throw (CanceledExecutionException)e;
			}
			errors.add(e.getMessage());
			LOGGER.error(e);
			return new ValueReport<PortObject[]>(null, errors, warnings);
		}
	}


	private void runRScript(final PortObject[] inData, final File tempWorkspaceFile, final ExecutionContext exec) throws Exception {
    	
    	String rScript = buildRScript(inData, tempWorkspaceFile);

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
	        
	        exec.setMessage("Execute R in external process");
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
        File tempCommandFile = FileUtil.createTempFile("R-inDataTempFile-", ".r");
        tempCommandFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempCommandFile);
        fw.write(cmd);
        fw.close();
        return tempCommandFile;
    }
    
	private String buildRScript(final PortObject[] inPorts, final File tempWorkspaceFile) throws BadLocationException {
    	StringBuilder rScript = new StringBuilder();
		// set working directory
		rScript.append("setwd(\"");
		rScript.append(tempWorkspaceFile.getParentFile());
		rScript.append("\");\n");
		
		// load workspaces from the input ports
		for (PortObject port : inPorts) {
			if (port instanceof RPortObject) {
				final RPortObject rPortObject = (RPortObject)port;
                File portFile = rPortObject.getFile();
				rScript.append("load(\"");
				rScript.append(portFile.getAbsolutePath().replace('\\', '/'));
				rScript.append("\");\n");
				rScript.append(RController.createLoadLibraryFunctionCall(rPortObject.getLibraries(), false));
			}
		}
		// load workspace with data table input and flow variables
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
		// assign list of loaded libraries so that we can read it out later
		rScript.append("\n" + RController.R_LOADED_LIBRARIES_VARIABLE + " <- (.packages());\n");
		// save workspace to temporary file
		rScript.append("save.image(\"");
		rScript.append(tempWorkspaceFile.getAbsolutePath().replace('\\', '/'));
		rScript.append("\");\n");
		
		return rScript.toString();
	}

	private BufferedDataTable importDataFromR(final RController r, final ExecutionContext exec) 
            throws REngineException, REXPMismatchException, CanceledExecutionException {
    	BufferedDataTable out = r.importBufferedDataTable("knime.out", exec);
    	return out;
    }

    private void importFlowVariablesFromR(final RController r, final FlowVariableRepository flowVarRepo,
			final ExecutionContext exec) throws CanceledExecutionException {
    	Collection<FlowVariable> flowVars = r.importFlowVariables("knime.flow.out", exec);
    	for (FlowVariable flowVar : flowVars) {
    		flowVarRepo.put(flowVar);
    	}
    }
    private List<String> importListOfLibrariesFromR(final RController r) {
        return r.importListOfLibrariesAndDelete();
    }

	private File exportData(final PortObject[] inData, final FlowVariableRepository flowVarRepo, final ExecutionContext exec) throws REngineException, REXPMismatchException, IOException, CanceledExecutionException {
    	RController r = RController.getDefault();
    	// TODO: lock controller
		r.clearWorkspace(exec);
		BufferedDataTable inTable = null;
		for (PortObject in : inData) {
	      	if (in instanceof BufferedDataTable) {
	      		inTable = (BufferedDataTable)in;
	       	}
	    }
		if (inTable != null) {
			r.exportDataTable(inTable, "knime.in", exec);
		}
		r.exportFlowVariables(flowVarRepo.getInFlowVariables(), "knime.flow.in", exec);
		
		// save workspace to temporary file
		File tempWorkspaceFile = FileUtil.createTempFile("R-workspace", ".RData");
		r.saveWorkspace(tempWorkspaceFile, exec);
		
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

	
	public RSnippetSettings getSettings() {
		return m_snippet.getSettings();
	}
	
	protected RSnippetNodeConfig getRSnippetNodeConfig() {
		return m_config;
	}
	
	protected RSnippet getRSnippet() {
		return m_snippet;
	}

	public void loadSettings(final RSnippetSettings settings) {
		m_snippet.getSettings().loadSettings(settings);
	}

}
