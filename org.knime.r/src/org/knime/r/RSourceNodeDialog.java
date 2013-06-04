/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.r;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ViewUtils;
import org.knime.r.ui.RColumnList;
import org.knime.r.ui.RConsole;
import org.knime.r.ui.RFlowVariableList;
import org.knime.r.ui.RObjectBrowser;
import org.knime.r.ui.RSnippetTextArea;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;



/**
 * The dialog of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class RSourceNodeDialog extends DataAwareNodeDialogPane implements RListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            RSourceNodeDialog.class);

    private static final String SNIPPET_TAB = "R Snippet";

    private RSnippetTextArea m_snippetTextArea;
    /** Component with a list of all input columns. */
    protected RColumnList m_colList;
    /** Component with a list of all input flow variables. */
    protected RFlowVariableList m_flowVarsList;

    /** The settings. */
    protected RSnippetSettings m_settings;
    private RSnippet m_snippet;

    /** The input data table */
	private BufferedDataTable m_data;

	private RConsole m_console;

	private RObjectBrowser m_objectBrowser;


    /**
     * Create a new Dialog.
     */
    @SuppressWarnings("rawtypes")
    protected RSourceNodeDialog() {
        m_settings = new RSnippetSettings();
        m_snippet = new RSnippet();
        
        JPanel panel = createPanel();
        m_colList.install(m_snippetTextArea);
        m_flowVarsList.install(m_snippetTextArea);
        
        addTab(SNIPPET_TAB, panel);
        panel.setPreferredSize(new Dimension(800, 600));
    }

    private JPanel createPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JComponent snippet = createSnippetPanel();
        JPanel snippetPanel = new JPanel(new BorderLayout());
        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton runButton = new JButton("Run Script");
        runButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					rClearRWorkspace();
					RController.getDefault().getConsoleQueue().putRScript(m_snippetTextArea.getText());
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		});
        runPanel.add(runButton);
        JButton evalSelButton = new JButton("Eval Selection");
        evalSelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					String selected = m_snippetTextArea.getSelectedText();
					if (selected != null) {
						RController.getDefault().getConsoleQueue().putRScript(selected);
					}
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		});
        runPanel.add(evalSelButton);        
        snippetPanel.add(snippet, BorderLayout.CENTER);
        snippetPanel.add(runPanel, BorderLayout.SOUTH);
        JComponent colsAndVars = createColsAndVarsPanel();

        
        
        JSplitPane leftSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT);
        leftSplitPane.setLeftComponent(colsAndVars);
        leftSplitPane.setRightComponent(snippetPanel);
        leftSplitPane.setDividerLocation(170);
        
        m_objectBrowser = new RObjectBrowser();
		m_objectBrowser.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = m_objectBrowser.getSelectedRow();
					if (row > -1) {
						String name = (String)m_objectBrowser.getValueAt(row, 0);
						String cmd = "print(" + name + ")";
						try {
							RController.getDefault().getConsoleQueue().putRScript(cmd);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		});
        JScrollPane objectBrowserScroller = new JScrollPane(m_objectBrowser);
        objectBrowserScroller.setBorder(createEmptyTitledBorder("Workspace"));
        
        JSplitPane rightSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT);
        rightSplitPane.setLeftComponent(leftSplitPane);
        rightSplitPane.setRightComponent(objectBrowserScroller);
        rightSplitPane.setDividerLocation(550);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(rightSplitPane);
        
        m_console = new RConsole();
        JScrollPane consoleScroller = new JScrollPane(m_console);
        consoleScroller.setBorder(createEmptyTitledBorder("Console"));
        mainSplitPane.setBottomComponent(consoleScroller);
        mainSplitPane.setOneTouchExpandable(true);

        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        centerPanel.add(mainSplitPane);

        p.add(centerPanel, BorderLayout.CENTER);
        return p;
    }


 
    /**
     * Create the panel with the snippet.
     */
    private JComponent createSnippetPanel() {

        m_snippetTextArea = new RSnippetTextArea(m_snippet);

//        // reset style which causes a recreation of the folds
//        m_snippetTextArea.setSyntaxEditingStyle(
//                SyntaxConstants.SYNTAX_STYLE_NONE);
//        m_snippetTextArea.setSyntaxEditingStyle(
//                SyntaxConstants.SYNTAX_STYLE_JAVA);
//        // collapse all folds
//        int foldCount = m_snippetTextArea.getFoldManager().getFoldCount();
//        for (int i = 0; i < foldCount; i++) {
//            Fold fold = m_snippetTextArea.getFoldManager().getFold(i);
//            fold.setCollapsed(true);
//        }
        JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
        snippetScroller.setBorder(createEmptyTitledBorder("R Script"));
        JPanel snippet = new JPanel(new BorderLayout());
        snippet.add(snippetScroller, BorderLayout.CENTER);
        ErrorStrip es = new ErrorStrip(m_snippetTextArea);
        snippet.add(es, BorderLayout.LINE_END);
        return snippet;
    }

    /**
     * The panel at the left with the column and variables at the input.
     * Override this method when the columns or variables should not be
     * displayed.
     * @return the panel at the left with the column and variables at the
     * input.
     */
    protected JComponent createColsAndVarsPanel() {
        JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_colList = new RColumnList();

        JScrollPane colListScroller = new JScrollPane(m_colList);
        colListScroller.setBorder(createEmptyTitledBorder("Column List"));
        varSplitPane.setTopComponent(colListScroller);

        // set variable panel
        m_flowVarsList = new RFlowVariableList();
        JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(
                createEmptyTitledBorder("Flow Variable List"));
        varSplitPane.setBottomComponent(flowVarScroller);
        varSplitPane.setOneTouchExpandable(true);
        varSplitPane.setResizeWeight(0.9);

        return varSplitPane;
    }
    
	/**
     * Create Panel with additional options to be displayed in the south.
     * @return options panel or null if there are no additional options.
     */
    protected JPanel createOptionsPanel() {
        return null;
    }

    /** Create an empty, titled border.
     * @param title Title of the border.
     * @return Such a new border.
     */
    protected Border createEmptyTitledBorder(final String title) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(
                5, 0, 0, 0), title, TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.BELOW_TOP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        // do not close on ESC, since ESC is used to close autocomplete popups
        // in the snippets textarea.
        return false;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
    		final BufferedDataTable[] input) throws NotConfigurableException {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                loadSettingsFromInternal(settings, input);
            }
        });
    }


    /**
     * Load settings invoked from the EDT-Thread.
     * @param settings the settings to load
     * @param specs the specs of the input table
     */
    protected void loadSettingsFromInternal(final NodeSettingsRO settings,
            final BufferedDataTable[] input) {
    	DataTableSpec spec = input[0].getSpec();
        m_settings.loadSettingsForDialog(settings);

        m_colList.setSpec(spec);
        m_flowVarsList.setFlowVariables(getAvailableFlowVariables().values());
        m_snippet.setSettings(m_settings);
        m_data = input[0];

//        // set caret position to the start of the custom expression
//        m_snippetTextArea.setCaretPosition(
//                m_snippet.getDocument().getGuardedSection(
//                RSnippetDocument.GUARDED_BODY_START).getEnd().getOffset()
//                + 1);
        m_snippetTextArea.requestFocusInWindow();
    }
    
    private void rClearRWorkspace() {
//		try {
//			RController.getDefault().clearWorkspace();
//		} catch (REngineException | REXPMismatchException e) {
//			// TODO: Add log entry
//		}	
    }

    private void rPrintValue(final String name) {
    	REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("print(" + name + ")");
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
		}	
    }
    
    private String[] rGetObjectNames() {
    	REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("ls()");
			return rexp != null ? rexp.asStrings() : null;
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
			return null;
		}					
	}
    
    private String[] rGetObjectClasses() {
    	REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("sapply(ls(),function(a)class(get(a,envir=globalenv()))[1])");
			return rexp != null ? rexp.asStrings() : null;
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
			return null;
		}					
	}
    
    
	@Override
	public void workspaceChanged(final REvent e) {
		final String[] objectNames = rGetObjectNames();
		if(objectNames != null && objectNames.length > 0){
			final String[] objectClasses = rGetObjectClasses();
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					m_objectBrowser.updateData(objectNames, objectClasses);
				}
			});
			
		}
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {
        // FIXME: Should we keep following lines?
        rClearRWorkspace();
        m_console.setText("");
        m_objectBrowser.updateData(new String[0], new String[0]);
        
        m_snippetTextArea.requestFocus();
        m_snippetTextArea.requestFocusInWindow();


        RController.getDefault().getConsoleController().attachOutput(m_console);
        // start listing to the RController for updating the object browser
        RController.getDefault().addRListener(this);
        

    }
    


	@Override
    public void onClose() {
    	RController.getDefault().getConsoleController().detach(m_console);
    	// start listing to the RController for updating the object browser
        RController.getDefault().removeRListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        RSnippetSettings s = m_snippet.getSettings();

        // give subclasses the chance to modify settings
        preSaveSettings(s);

        s.saveSettings(settings);
    }

    /**
     * Called right before storing the settings object. Gives subclasses
     * the chance to modify the settings object.
     * @param s the settings
     */
    protected void preSaveSettings(final RSnippetSettings s) {
        // just a place holder.
    }
}
