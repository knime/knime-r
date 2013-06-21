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
 */
package org.knime.r;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.r.node.local.port.RPortObject;
import org.knime.r.template.AddTemplateDialog;
import org.knime.r.template.TemplateProvider;
import org.knime.r.ui.RColumnList;
import org.knime.r.ui.RConsole;
import org.knime.r.ui.RFlowVariableList;
import org.knime.r.ui.RObjectBrowser;
import org.knime.r.ui.RProgressPanel;
import org.knime.r.ui.RSnippetTextArea;
import org.rosuda.REngine.REXP;

/**
 * The dialog component for RSnippet-Nodes.
 * 
 * @author Heiko Hofer
 */
public class RSnippetNodePanel extends JPanel implements RListener {
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RSnippetNodeDialog.class);

	private static final String SNIPPET_TAB = "R Snippet";

	private RSnippetTextArea m_snippetTextArea;
	/** Component with a list of all input columns. */
	protected RColumnList m_colList;
	/** Component with a list of all input flow variables. */
	protected RFlowVariableList m_flowVarsList;

	private RSnippet m_snippet;

	/** The input data table */
	private BufferedDataTable m_data;

	private RConsole m_console;

	private RObjectBrowser m_objectBrowser;

	private boolean m_isEnabled;
	private boolean m_isInteractive;

	@SuppressWarnings("rawtypes")
	/** The templates category for templates viewed or edited by this dialog. */
	protected Class m_templateMetaCategory;
	private JLabel m_templateLocation;

	private RSnippetNodeConfig m_config;

	private int m_tableInPort;

	private PortObject[] m_input;

	private Collection<FlowVariable> m_inputFlowVars;

	private RProgressPanel m_progressPanel;

	private boolean m_hasLock;

	private JButton m_evalSelButton;

	private JButton m_evalScriptButton;

	private JButton m_resetWorkspace;

	private final ReentrantLock m_lock;
	private ExecutionMonitor m_exec;

	protected boolean m_closing;

	/**
	 * @param templateMetaCategory
	 *            the meta category used in the templates tab or to create
	 *            templates
	 * @param config
	 * @param isPreview
	 *            if this is a preview used for showing templates.
	 */
	@SuppressWarnings("rawtypes")
	public RSnippetNodePanel(final Class templateMetaCategory,
			final RSnippetNodeConfig config, final boolean isPreview,
			final boolean isInteractive) {
		super(new BorderLayout());
		m_lock = new ReentrantLock();
		m_config = config;
		m_tableInPort = -1;
		int i = 0;
		for (PortType portType : m_config.getInPortTypes()) {
			if (portType.equals(BufferedDataTable.TYPE)) {
				m_tableInPort = i;
			}
			i++;
		}

		m_templateMetaCategory = templateMetaCategory;
		m_isEnabled = true;

		m_isInteractive = isPreview ? false : isInteractive;

		m_snippet = new RSnippet();

		JPanel panel = createPanel(isPreview, m_isInteractive);
		m_colList.install(m_snippetTextArea);
		m_flowVarsList.install(m_snippetTextArea);

		setEnabled(!isPreview);
		panel.setPreferredSize(new Dimension(850, 600));
	}

	private JPanel createPanel(final boolean isPreview,
			final boolean isInteractive) {
		JPanel p = this;
		JComponent snippet = createSnippetPanel();
		JPanel snippetPanel = new JPanel(new BorderLayout());
		snippetPanel.add(snippet, BorderLayout.CENTER);

		if (isInteractive) {
			JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			m_evalScriptButton = new JButton("Eval Script");
			m_evalScriptButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						RController.getDefault().getConsoleQueue()
								.putRScript(m_snippetTextArea.getText());
					} catch (InterruptedException e1) {
						throw new RuntimeException(e1);
					}
				}
			});
			runPanel.add(m_evalScriptButton);
			m_evalSelButton = new JButton("Eval Selection");
			m_evalSelButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						String selected = m_snippetTextArea.getSelectedText();
						if (selected != null) {
							RController.getDefault().getConsoleQueue()
									.putRScript(selected);
						}
					} catch (InterruptedException e1) {
						throw new RuntimeException(e1);
					}
				}
			});
			runPanel.add(m_evalSelButton);
			snippetPanel.add(runPanel, BorderLayout.SOUTH);
		}

		JComponent colsAndVars = createColsAndVarsPanel();

		JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		leftSplitPane.setLeftComponent(colsAndVars);
		leftSplitPane.setRightComponent(snippetPanel);
		leftSplitPane.setDividerLocation(170);

		m_objectBrowser = new RObjectBrowser();
		m_console = new RConsole();

		if (isInteractive) {
			m_objectBrowser.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					if (e.getClickCount() == 2) {
						int row = m_objectBrowser.getSelectedRow();
						if (row > -1) {
							String name = (String) m_objectBrowser.getValueAt(
									row, 0);
							String cmd = "print(" + name + ")";
							try {
								RController.getDefault().getConsoleQueue()
										.putRScript(cmd);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			});
			JScrollPane objectBrowserScroller = new JScrollPane(m_objectBrowser);
			objectBrowserScroller
					.setBorder(createEmptyTitledBorder("Workspace"));

			JPanel objectBrowserContainer = new JPanel(new BorderLayout());
			objectBrowserContainer.add(objectBrowserScroller,
					BorderLayout.CENTER);

			JPanel objectBrowserButtons = new JPanel(new FlowLayout(
					FlowLayout.TRAILING));
			m_resetWorkspace = new JButton("Reset Workspace");
			m_resetWorkspace.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					resetWorkspace();
				}
			});
			objectBrowserButtons.add(m_resetWorkspace);
			objectBrowserContainer
					.add(objectBrowserButtons, BorderLayout.SOUTH);

			JSplitPane rightSplitPane = new JSplitPane(
					JSplitPane.HORIZONTAL_SPLIT);
			rightSplitPane.setLeftComponent(leftSplitPane);
			rightSplitPane.setRightComponent(objectBrowserContainer);
			rightSplitPane.setDividerLocation(550);

			JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			rightSplitPane.setPreferredSize(new Dimension(rightSplitPane
					.getPreferredSize().width, 400));
			mainSplitPane.setTopComponent(rightSplitPane);

			JScrollPane consoleScroller = new JScrollPane(m_console);
			consoleScroller.setBorder(createEmptyTitledBorder("Console"));
			mainSplitPane.setBottomComponent(consoleScroller);
			mainSplitPane.setOneTouchExpandable(true);

			JPanel centerPanel = new JPanel(new GridLayout(0, 1));
			centerPanel.add(mainSplitPane);

			m_progressPanel = new RProgressPanel();

			p.add(centerPanel, BorderLayout.CENTER);
			p.add(m_progressPanel, BorderLayout.SOUTH);
		} else {
			p.add(leftSplitPane, BorderLayout.CENTER);
		}
		JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
		p.add(templateInfoPanel, BorderLayout.NORTH);
		return p;
	}

	/**
	 * Reset workspace with input data.
	 */
	private void resetWorkspace() {
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				m_resetWorkspace.setEnabled(false);
				try {
					while (!m_lock.tryLock(100, TimeUnit.MILLISECONDS)) {
						if (m_closing) {
							return null;
						}
					}
					try {
						// release some references to prevent memory leaks
						if (m_exec != null) {
							m_exec.getProgressMonitor()
									.removeAllProgressListener();
						}
						m_exec = new ExecutionMonitor(
								new DefaultNodeProgressMonitor());
						m_progressPanel.startMonitoring(m_exec);
						RController.getDefault().clearWorkspace(m_exec);

						if (m_input != null) {
							for (int i = 0; i < m_input.length; i++) {
								if (m_input[i] instanceof RPortObject) {
									m_exec.setMessage("Load R data from input.");
									RPortObject port = (RPortObject) m_input[i];
									RController.getDefault().loadWorkspace(
											port.getFile(), m_exec);
								}
							}
						}

						if (m_input != null && m_tableInPort >= 0) {
							m_exec.setMessage("Send input table to R");
							RController.getDefault().exportDataTable(
									(BufferedDataTable) m_input[m_tableInPort],
									"knime.in", m_exec);
						}

						m_exec.setMessage("Send flow variables to R");
						RController.getDefault().exportFlowVariables(
								m_inputFlowVars, "knime.flow.in", m_exec);
						workspaceChanged(null);

					} finally {
						if (m_exec != null) {
							m_exec.getProgressMonitor().setExecuteCanceled();
						}
						m_lock.unlock();
					}

				} finally {
					m_resetWorkspace.setEnabled(true);
				}
				return null;
			}

		}.execute();
	}

	/**
	 * Create the panel with the snippet.
	 */
	private JComponent createSnippetPanel() {

		m_snippetTextArea = new RSnippetTextArea(m_snippet);

		// // reset style which causes a recreation of the folds
		// m_snippetTextArea.setSyntaxEditingStyle(
		// SyntaxConstants.SYNTAX_STYLE_NONE);
		// m_snippetTextArea.setSyntaxEditingStyle(
		// SyntaxConstants.SYNTAX_STYLE_JAVA);
		// // collapse all folds
		// int foldCount = m_snippetTextArea.getFoldManager().getFoldCount();
		// for (int i = 0; i < foldCount; i++) {
		// Fold fold = m_snippetTextArea.getFoldManager().getFold(i);
		// fold.setCollapsed(true);
		// }
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
	 * 
	 * @return the panel at the left with the column and variables at the input.
	 */
	protected JComponent createColsAndVarsPanel() {
		JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		m_colList = new RColumnList();

		// set variable panel
		m_flowVarsList = new RFlowVariableList();
		JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
		flowVarScroller
				.setBorder(createEmptyTitledBorder("Flow Variable List"));

		if (m_tableInPort >= 0) {
			JScrollPane colListScroller = new JScrollPane(m_colList);
			colListScroller.setBorder(createEmptyTitledBorder("Column List"));
			varSplitPane.setTopComponent(colListScroller);
			varSplitPane.setBottomComponent(flowVarScroller);
			varSplitPane.setOneTouchExpandable(true);
			varSplitPane.setResizeWeight(0.9);

			return varSplitPane;
		} else {
			return flowVarScroller;
		}
	}

	/**
	 * Create Panel with additional options to be displayed in the south.
	 * 
	 * @return options panel or null if there are no additional options.
	 */
	protected JPanel createOptionsPanel() {
		return null;
	}

	/**
	 * The panel at the to with the "Create Template..." Button.
	 */
	private JPanel createTemplateInfoPanel(final boolean isPreview) {
		final JButton addTemplateButton = new JButton("Create Template...");
		addTemplateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Frame parent = (Frame) SwingUtilities.getAncestorOfClass(
						Frame.class, addTemplateButton);
				RSnippetTemplate newTemplate = AddTemplateDialog
						.openUserDialog(parent, m_snippet,
								m_templateMetaCategory);
				if (null != newTemplate) {
					TemplateProvider.getDefault().addTemplate(newTemplate);
					// update the template UUID of the current snippet
					m_snippet.getSettings().setTemplateUUID(
							newTemplate.getUUID());
					String loc = TemplateProvider.getDefault()
							.getDisplayLocation(newTemplate);
					m_templateLocation.setText(loc);
					validate();
				}
			}
		});
		JPanel templateInfoPanel = new JPanel(new BorderLayout());
		TemplateProvider provider = TemplateProvider.getDefault();
		String uuid = m_snippet.getSettings().getTemplateUUID();
		RSnippetTemplate template = null != uuid ? provider.getTemplate(UUID
				.fromString(uuid)) : null;
		String loc = null != template ? createTemplateLocationText(template)
				: "";
		m_templateLocation = new JLabel(loc);
		if (isPreview) {
			templateInfoPanel.add(m_templateLocation, BorderLayout.CENTER);
		} else {
			templateInfoPanel.add(addTemplateButton, BorderLayout.LINE_END);
		}
		templateInfoPanel
				.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		return templateInfoPanel;
	}

	/**
	 * Create an empty, titled border.
	 * 
	 * @param title
	 *            Title of the border.
	 * @return Such a new border.
	 */
	protected Border createEmptyTitledBorder(final String title) {
		return BorderFactory.createTitledBorder(
				BorderFactory.createEmptyBorder(5, 0, 0, 0), title,
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
	}

	/**
	 * Determines whether this component is enabled. An enabled component can
	 * respond to user input and generate events.
	 * 
	 * @return <code>true</code> if the component is enabled, <code>false</code>
	 *         otherwise
	 */
	@Override
	public boolean isEnabled() {
		return m_isEnabled;
	}

	/**
	 * Sets whether or not this component is enabled. A component that is
	 * enabled may respond to user input, while a component that is not enabled
	 * cannot respond to user input.
	 * 
	 * @param enabled
	 *            true if this component should be enabled, false otherwise
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		if (m_isEnabled != enabled) {
			m_colList.setEnabled(enabled);
			m_flowVarsList.setEnabled(enabled);
			m_snippetTextArea.setEnabled(enabled);
			m_objectBrowser.setEnabled(enabled);
			m_console.setEnabled(enabled);
		}
		m_isEnabled = enabled;

	}

	private void rClearRWorkspace() {
		ExecutionMonitor exec = new ExecutionMonitor();
		try {
			RController.getDefault().clearWorkspace(exec);
		} catch (CanceledExecutionException e) {
			LOGGER.info("clear workspace canceled.", e);
		}
	}

	private void rPrintValue(final String name) {
		REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("print(" + name + ")");
		} catch (Exception e) {
			// TODO: Add log entry
		}
	}

	private String[] rGetObjectNames() {
		REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("ls()");
			return rexp != null ? rexp.asStrings() : null;
		} catch (Exception e) {
			// TODO: Add log entry
			return null;
		}
	}

	private String[] rGetObjectClasses() {
		REXP rexp;
		try {
			rexp = RController
					.getDefault()
					.idleEval(
							"sapply(ls(),function(a)class(get(a,envir=globalenv()))[1])");
			return rexp != null ? rexp.asStrings() : null;
		} catch (Exception e) {
			// TODO: Add log entry
			return null;
		}
	}

	@Override
	public void workspaceChanged(final REvent e) {
		final String[] objectNames = rGetObjectNames();
		if (objectNames != null && objectNames.length > 0) {
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
	 * Reinitialize with the given blueprint.
	 * 
	 * @param template
	 *            the template
	 * @param flowVariables
	 *            the flow variables at the input
	 * @param spec
	 *            the input spec
	 */
	public void applyTemplate(final RSnippetTemplate template,
			final DataTableSpec spec,
			final Map<String, FlowVariable> flowVariables) {
		// save and read settings to decouple objects.
		NodeSettings settings = new NodeSettings(template.getUUID());
		template.getSnippetSettings().saveSettings(settings);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			settings.saveToXML(os);
			NodeSettingsRO settingsro = NodeSettings
					.loadFromXML(new ByteArrayInputStream(os.toString("UTF-8")
							.getBytes("UTF-8")));
			m_snippet.getSettings().loadSettings(settingsro);
		} catch (Exception e) {
			LOGGER.error("Cannot apply template.", e);
		}

		m_colList.setSpec(spec);
		m_flowVarsList.setFlowVariables(flowVariables.values());
		// update template info panel
		m_templateLocation.setText(createTemplateLocationText(template));

		m_snippetTextArea.requestFocus();
	}

	/**
	 * Get the template's location for display.
	 * 
	 * @param template
	 *            the template
	 * @return the template's loacation for display
	 */
	private String createTemplateLocationText(final RSnippetTemplate template) {
		TemplateProvider provider = TemplateProvider.getDefault();
		return provider.getDisplayLocation(template);
	}

	/**
	 * {@inheritDoc}
	 */
	public ValueReport<Boolean> onOpen() {
		m_closing = false;
		if (m_isInteractive) {

			m_console.setText("");
			m_objectBrowser.updateData(new String[0], new String[0]);

			m_snippetTextArea.requestFocus();
			m_snippetTextArea.requestFocusInWindow();

			final ValueReport<Boolean> isRAvailable = RController.getDefault().isRAvailable();
			m_hasLock = isRAvailable.getValue() ? RController.getDefault().tryAcquire() : false;
			try {
				m_evalScriptButton.setEnabled(m_hasLock);
				m_evalSelButton.setEnabled(m_hasLock);
				m_resetWorkspace.setEnabled(m_hasLock);
				if (isRAvailable.getValue()) {
					if (!m_hasLock) {
						new SwingWorker<Void, Void>() {

							@Override
							protected Void doInBackground() throws Exception {
								while (!m_lock.tryLock(100, TimeUnit.MILLISECONDS)) {
									if (m_closing) {
										return null;
									}
								}
								try {
									// release some references to prevent memory
									// leaks
									if (m_exec != null) {
										m_exec.getProgressMonitor().removeAllProgressListener();
									}
									m_exec = new ExecutionMonitor(new DefaultNodeProgressMonitor());
									m_progressPanel.startMonitoring(m_exec);
									m_exec.setMessage("R is busy waiting...");
									m_exec.setProgress(0.0);
									while (!m_hasLock) {
										m_exec.checkCanceled();
										m_hasLock = RController.getDefault().tryAcquire(500, TimeUnit.MILLISECONDS);
									}
									try {
										m_evalScriptButton.setEnabled(m_hasLock);
										m_evalSelButton.setEnabled(m_hasLock);
										m_resetWorkspace.setEnabled(m_hasLock);
										connectToR();
									} finally {
										if (m_hasLock) {
											RController.getDefault().release();
											m_hasLock = false;
										}
									}
								} finally {
									if (m_exec != null) {
										m_exec.getProgressMonitor().setExecuteCanceled();
									}
									m_lock.unlock();
								}
								return null;
							}
						}.execute();
					} else {
						connectToR();
					}
				}
			} finally {
				if (m_hasLock) {
					RController.getDefault().release();
					m_hasLock = false;
				}
			}
			return isRAvailable;
		} else {
			return new ValueReport<Boolean>(true, Collections.EMPTY_LIST,
					Collections.EMPTY_LIST);
		}

	}

	private void connectToR() {
		RController.getDefault().getConsoleController().attachOutput(m_console);
		// start listing to the RController for updating the object browser
		RController.getDefault().addRListener(this);

		// send data to R
		resetWorkspace();
	}

	public void onClose() {
		m_closing = true;
		m_progressPanel.stopMonitoring();
		if (m_isInteractive) {
			try {
				if (RController.getDefault().isRAvailable().getValue()) {
					if (RController.getDefault().getConsoleController().isAttached(m_console)) {
						RController.getDefault().getConsoleController().detach(m_console);
					}
					// stop listing to the RController for updating the object
					// browser
					RController.getDefault().removeRListener(this);
					// Stop running tasks
					m_exec.getProgressMonitor().setExecuteCanceled();
				}
			} finally {
				if (m_hasLock) {
					RController.getDefault().release();
					m_hasLock = false;
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */

	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		RSnippetSettings s = m_snippet.getSettings();

		// give subclasses the chance to modify settings
		preSaveSettings(s);

		s.saveSettings(settings);
	}

	/**
	 * Called right before storing the settings object. Gives subclasses the
	 * chance to modify the settings object.
	 * 
	 * @param s
	 *            the settings
	 */
	protected void preSaveSettings(final RSnippetSettings s) {
		// just a place holder.
	}

	public void updateData(final NodeSettingsRO settings,
			final PortObjectSpec[] specs,
			final Collection<FlowVariable> flowVariables) {
		m_snippet.getSettings().loadSettingsForDialog(settings);
		DataTableSpec spec = m_tableInPort >= 0 ? (DataTableSpec) specs[m_tableInPort]
				: null;
		updateData(m_snippet.getSettings(), null, spec, flowVariables);
	}

	public void updateData(final NodeSettingsRO settings,
			final PortObject[] input,
			final Collection<FlowVariable> flowVariables) {
		m_snippet.getSettings().loadSettingsForDialog(settings);
		DataTableSpec spec = m_tableInPort >= 0 ? ((BufferedDataTable) input[m_tableInPort])
				.getSpec() : null;
		updateData(m_snippet.getSettings(), input, spec, flowVariables);

	}

	private void updateData(final RSnippetSettings settings,
			final PortObject[] input, final DataTableSpec spec,
			final Collection<FlowVariable> flowVariables) {
		ViewUtils.invokeAndWaitInEDT(new Runnable() {
			@Override
			public void run() {
				updateDataInternal(settings, input, spec, flowVariables);
			}

		});

	}

	protected void updateDataInternal(final RSnippetSettings settings,
			final PortObject[] input, final DataTableSpec spec,
			final Collection<FlowVariable> flowVariables) {
		m_snippet.getSettings().loadSettings(settings);

		m_colList.setSpec(spec);
		m_flowVarsList.setFlowVariables(flowVariables);
		m_input = input;
		m_inputFlowVars = flowVariables;

		// // set caret position to the start of the custom expression
		// m_snippetTextArea.setCaretPosition(
		// m_snippet.getDocument().getGuardedSection(
		// RSnippetDocument.GUARDED_BODY_START).getEnd().getOffset()
		// + 1);
		m_snippetTextArea.requestFocusInWindow();

		// update template info panel
		TemplateProvider provider = TemplateProvider.getDefault();
		String uuid = m_snippet.getSettings().getTemplateUUID();
		RSnippetTemplate template = null != uuid ? provider.getTemplate(UUID
				.fromString(uuid)) : null;
		String loc = null != template ? createTemplateLocationText(template)
				: "";
		m_templateLocation.setText(loc);
	}

	public RSnippet getRSnippet() {
		return m_snippet;
	}

}
