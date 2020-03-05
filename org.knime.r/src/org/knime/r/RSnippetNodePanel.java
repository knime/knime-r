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
 */
package org.knime.r;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RCommand;
import org.knime.r.controller.RCommandQueue;
import org.knime.r.controller.RConsoleController;
import org.knime.r.controller.RController;
import org.knime.r.template.AddTemplateDialog;
import org.knime.r.template.TemplateProvider;
import org.knime.r.template.TemplateReceiver;
import org.knime.r.ui.RColumnList;
import org.knime.r.ui.RConsole;
import org.knime.r.ui.RFlowVariableList;
import org.knime.r.ui.RObjectBrowser;
import org.knime.r.ui.RProgressPanel;
import org.knime.r.ui.RSnippetTextArea;
import org.rosuda.REngine.REXP;

import com.sun.jna.Platform;

/**
 * The dialog component for RSnippet-Nodes.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
public class RSnippetNodePanel extends JPanel implements TemplateReceiver {

    /** Generated serialVersionUID */
    private static final long serialVersionUID = 2286323699400964363L;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RSnippetNodeDialog.class);

    private RSnippetTextArea m_snippetTextArea;

    /** Component with a list of all input columns. */
    protected RColumnList m_colList;

    /** Component with a list of all input flow variables. */
    protected RFlowVariableList m_flowVarsList;

    private final RSnippet m_snippet;

    private RConsole m_console;

    private RCommandQueue m_commandQueue;

    private RConsoleController m_consoleController;

    private RController m_controller;

    private RObjectBrowser m_objectBrowser;

    private boolean m_isEnabled;

    private final boolean m_isInteractive;

    /** The templates category for templates viewed or edited by this dialog. */
    protected Class<?> m_templateMetaCategory;

    private JLabel m_templateLocation;

    private final RSnippetNodeConfig m_config;

    private int m_tableInPort;

    private PortObject[] m_input;

    private Collection<FlowVariable> m_inputFlowVars;

    private RProgressPanel m_progressPanel;

    private JButton m_evalSelButton;

    private JButton m_evalScriptButton;

    private JButton m_resetWorkspace;

    private JButton m_showPlot;

    private JButton m_consoleCancelButton;

    private JButton m_consoleClearButton;

    private RPlotPreviewFrame m_previewFrame;

    private File m_tempDir;

    private File m_imageFile;

    private ExecutionMonitor m_exec;

    /** Whether the dialog is currently closing */
    protected boolean m_closing;

    /**
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     * @param config
     * @param isPreview if this is a preview used for showing templates.
     * @param isInteractive Whether the code should be interactively executable
     */
    public RSnippetNodePanel(final Class<?> templateMetaCategory, final RSnippetNodeConfig config,
        final boolean isPreview, final boolean isInteractive) {
        this(RPreferenceInitializer.getRProvider(), templateMetaCategory, config, isPreview, isInteractive);
    }

    /**
     * @param preferences the R preferences to use
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     * @param config
     * @param isPreview if this is a preview used for showing templates.
     * @param isInteractive Whether the code should be interactively executable
     */
    public RSnippetNodePanel(final RPreferenceProvider preferences, final Class<?> templateMetaCategory,
        final RSnippetNodeConfig config, final boolean isPreview, final boolean isInteractive) {
        super(new BorderLayout());
        m_config = config;
        m_tableInPort = -1;
        int i = 0;
        for (final PortType portType : m_config.getInPortTypes()) {
            if (portType.equals(BufferedDataTable.TYPE)) {
                m_tableInPort = i;
            }
            i++;
        }

        m_templateMetaCategory = templateMetaCategory;
        m_isEnabled = true;

        m_isInteractive = isPreview ? false : isInteractive;

        if (m_isInteractive) {
            createRController(preferences);
        } else {
            m_controller = null;
            m_consoleController = null;
            m_commandQueue = null;
        }

        m_snippet = new RSnippet();

        final JPanel panel = createPanel(isPreview, m_isInteractive);
        m_colList.install(m_snippetTextArea);
        m_flowVarsList.install(m_snippetTextArea);

        setEnabled(!isPreview);
        panel.setPreferredSize(new Dimension(1280, 720));
    }

    /**
     * Update the used preferences. The R controller gets created.
     *
     * @param preferences the new preferences
     * @since 4.2
     */
    public void updateRPreferences(final RPreferenceProvider preferences) {
        if (m_isInteractive) {
            detachConsoleController();
            closeRController();
            createRController(preferences);
            m_consoleCancelButton.setAction(m_consoleController.getCancelAction());
            m_consoleClearButton.setAction(m_consoleController.getClearAction());
        }
    }

    /** Create the RController, RCommandQueue and RConsoleController */
    private void createRController(final RPreferenceProvider preferences) {
        m_controller = new RController(true, preferences);
        m_commandQueue = new RCommandQueue(m_controller);
        m_consoleController = new RConsoleController(m_controller, m_commandQueue);
    }

    private JPanel createPanel(final boolean isPreview, final boolean isInteractive) {
        final JComponent snippet = createSnippetPanel();
        final JPanel snippetPanel = new JPanel(new BorderLayout());
        snippetPanel.add(snippet, BorderLayout.CENTER);

        if (isInteractive) {
            final JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            m_evalScriptButton = new JButton("Eval Script");
            m_evalScriptButton.addActionListener(e -> evalScriptFragment(m_snippetTextArea.getText()));
            runPanel.add(m_evalScriptButton);
            m_evalSelButton = new JButton("Eval Selection");
            m_evalSelButton.setToolTipText("Evaluate selected script or current line");
            m_evalSelButton.addActionListener(e -> {
                evalSelectedScript();
            });
            runPanel.add(m_evalSelButton);
            snippetPanel.add(runPanel, BorderLayout.SOUTH);
        }

        final JComponent colsAndVars = createColsAndVarsPanel();

        final JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplitPane.setLeftComponent(colsAndVars);
        leftSplitPane.setRightComponent(snippetPanel);
        leftSplitPane.setDividerLocation(200);

        m_objectBrowser = new RObjectBrowser();
        m_console = new RConsole();

        if (isInteractive) {
            m_objectBrowser.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        final int row = m_objectBrowser.getSelectedRow();
                        if (row > -1) {
                            final String name = (String)m_objectBrowser.getValueAt(row, 0);
                            final String cmd = "print(" + name + ")";
                            m_commandQueue.putRScript(cmd, true);
                        }
                    }
                }
            });
            final JScrollPane objectBrowserScroller = new JScrollPane(m_objectBrowser);
            objectBrowserScroller.setBorder(createEmptyTitledBorder("Workspace"));

            final JPanel objectBrowserContainer = new JPanel(new BorderLayout());
            objectBrowserContainer.add(objectBrowserScroller, BorderLayout.CENTER);

            final JPanel objectBrowserButtons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            m_resetWorkspace = new JButton("Reset Workspace");
            m_resetWorkspace.addActionListener(e -> resetWorkspace());
            objectBrowserButtons.add(m_resetWorkspace);

            m_showPlot = new JButton("Show Plot");
            m_showPlot.addActionListener(e -> showPlot());
            objectBrowserButtons.add(m_showPlot);

            objectBrowserContainer.add(objectBrowserButtons, BorderLayout.SOUTH);

            final JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            rightSplitPane.setLeftComponent(leftSplitPane);
            rightSplitPane.setRightComponent(objectBrowserContainer);
            rightSplitPane.setResizeWeight(1.0);
            rightSplitPane.setDividerLocation(1000);
            rightSplitPane.setOneTouchExpandable(true);

            final JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            rightSplitPane.setPreferredSize(new Dimension(rightSplitPane.getPreferredSize().width, 400));
            mainSplitPane.setTopComponent(rightSplitPane);

            final JPanel consolePanel = new JPanel(new BorderLayout());
            final JScrollPane consoleScroller = new JScrollPane(m_console);
            final JPanel consoleButtons = createConsoleButtons();

            consolePanel.add(consoleButtons, BorderLayout.WEST);
            consolePanel.add(consoleScroller, BorderLayout.CENTER);
            consolePanel.setBorder(createEmptyTitledBorder("Console"));
            mainSplitPane.setBottomComponent(consolePanel);
            mainSplitPane.setOneTouchExpandable(true);
            mainSplitPane.setResizeWeight(0.5);

            final JPanel centerPanel = new JPanel(new GridLayout(0, 1));
            centerPanel.add(mainSplitPane);

            m_progressPanel = new RProgressPanel();

            add(centerPanel, BorderLayout.CENTER);
            add(m_progressPanel, BorderLayout.SOUTH);
        } else {
            add(leftSplitPane, BorderLayout.CENTER);
        }
        final JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
        add(templateInfoPanel, BorderLayout.NORTH);
        return this;
    }

    private void evalSelectedScript() {
        final String selected = m_snippetTextArea.getSelectedText();
        if (selected != null) {
            evalScriptFragment(selected);
        } else {
            /* Emulate behaviour of R studio: evaluate current line the caret is on and move to next line */
            int currentLine = m_snippetTextArea.getCaretLineNumber();
            try {
                final int start = m_snippetTextArea.getLineStartOffset(currentLine);
                final int end = m_snippetTextArea.getLineEndOffset(currentLine);

                final String line = m_snippet.getDocument().getText(start, end - start);
                evalScriptFragment(line);

                /* Move to next line if not already */
                if (currentLine < m_snippetTextArea.getLineCount() - 1) {
                    final int nextLineStart = m_snippetTextArea.getLineStartOffset(currentLine + 1);
                    m_snippetTextArea.setCaretPosition(nextLineStart);
                }
                /* Focus text area again, so that button can be clicked repeatedly */
                m_snippetTextArea.requestFocus();
            } catch (BadLocationException e1) {
                // will never happen
                LOGGER.error("Bad location while trying to execute current line.", e1);
            }
        }
    }

    private JPanel createConsoleButtons() {

        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(4, 2, 0, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        m_consoleCancelButton = new JButton(m_consoleController.getCancelAction());
        m_consoleCancelButton.setText("");
        m_consoleCancelButton.setPreferredSize(new Dimension(m_consoleCancelButton.getPreferredSize().height,
            m_consoleCancelButton.getPreferredSize().height));

        p.add(m_consoleCancelButton, c);

        m_consoleClearButton = new JButton(m_consoleController.getClearAction());
        m_consoleClearButton.setText("");
        m_consoleClearButton.setPreferredSize(
            new Dimension(m_consoleClearButton.getPreferredSize().height, m_consoleClearButton.getPreferredSize().height));

        c.gridy++;
        p.add(m_consoleClearButton, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);
        return p;
    }

    /**
     * Reset workspace with input data.
     */
    private void resetWorkspace() {
        new SwingWorkerWithContext<Void, Boolean>() {

            @Override
            protected void processWithContext(final List<Boolean> chunks) {
                // disable dialog components
                enableComponents(chunks.get(chunks.size() - 1));
            }

            @Override
            protected Void doInBackgroundWithContext() throws Exception {
                publish(Boolean.FALSE);
                try {
                    try {
                        // release some references to prevent memory leaks
                        if (m_exec != null) {
                            m_exec.getProgressMonitor().removeAllProgressListener();
                        }
                        m_exec = new ExecutionMonitor(new DefaultNodeProgressMonitor());
                        m_progressPanel.startMonitoring(m_exec);

                        // no need to load any R workspace
                        m_controller.clearWorkspace(m_exec.createSubProgress(0.3));

                        if (m_input != null) {
                            m_exec.setMessage("Sending input data to R");
                            final RSnippetSettings s = m_snippet.getSettings();
                            m_controller.importDataFromPorts(m_input, m_exec.createSubProgress(0.6),
                                s.getSendBatchSize(), s.getKnimeInType(), s.getSendRowNames());
                        }

                        m_exec.setMessage("Sending flow variables to R");
                        m_controller.exportFlowVariables(m_inputFlowVars, "knime.flow.in",
                            m_exec.createSubProgress(0.1));

                        workspaceChanged();
                    } finally {
                        if (m_exec != null) {
                            m_exec.getProgressMonitor().setExecuteCanceled();
                            // make sure the m_exec is at 100% at end
                            m_exec.setProgress(1);
                        }
                    }
                } finally {
                    publish(Boolean.TRUE);
                }
                return null;
            }
        }.execute();
    }

    private String setupPlotInitCommand() {
        final StringBuilder b = new StringBuilder();
        if (m_imageFile != null) {
            String bitmapType = "";

            if (Platform.isMac()) {
                bitmapType = ",bitmapType='cairo'";
                b.append("library('Cairo');");
            }

            final Dimension size = getPreviewImageDimensions();
            b.append("options(device='png'" + bitmapType + ")").append("\n");
            b.append("png(\"" + m_imageFile.getAbsolutePath().replace('\\', '/') + "\",width=" + size.getWidth()
                + ",height=" + size.getHeight() + ")").append("\n");
        }
        return b.toString();
    }

    private void showPlot() {
        boolean isToPack = false;
        if (m_previewFrame == null) {
            m_previewFrame = new RPlotPreviewFrame((Frame)SwingUtilities.getAncestorOfClass(Frame.class, this));
            isToPack = true;
        }
        try {
            m_previewFrame.setSource(m_imageFile);
        } catch (final IOException e) {
            LOGGER.error("Unable to render PNG/SVG from file " + m_imageFile.getAbsolutePath(), e);
        }
        if (isToPack) {
            m_previewFrame.pack();
            if ((m_imageFile == null) || (m_imageFile.length() == 0L)) {
                m_previewFrame.setSize(getPreviewImageDimensions());
            }
        }
        m_previewFrame.setVisible(true);
    }

    /**
     * Override to change preview image dimensions. Default is `(400, 300)`.
     *
     * @return dimensions of the preview image.
     */
    protected Dimension getPreviewImageDimensions() {
        return new Dimension(400, 300);
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
        final JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
        snippetScroller.setBorder(createEmptyTitledBorder("R Script"));
        final JPanel snippet = new JPanel(new BorderLayout());
        snippet.add(snippetScroller, BorderLayout.CENTER);
        final ErrorStrip es = new ErrorStrip(m_snippetTextArea);
        snippet.add(es, BorderLayout.LINE_END);
        return snippet;
    }

    /**
     * The panel at the left with the column and variables at the input. Override this method when the columns or
     * variables should not be displayed.
     *
     * @return the panel at the left with the column and variables at the input.
     */
    protected JComponent createColsAndVarsPanel() {
        final JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_colList = new RColumnList();

        // set variable panel
        m_flowVarsList = new RFlowVariableList();
        final JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(createEmptyTitledBorder("Flow Variable List"));

        if (m_tableInPort >= 0) {
            final JScrollPane colListScroller = new JScrollPane(m_colList);
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
     * The panel at the top with the "Create Template..." Button.
     */
    private JPanel createTemplateInfoPanel(final boolean isPreview) {
        final JButton addTemplateButton = new JButton("Create Template...");
        addTemplateButton.addActionListener(e -> {
            final Frame parent = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, addTemplateButton);
            final RSnippetTemplate newTemplate =
                AddTemplateDialog.openUserDialog(parent, m_snippet, m_templateMetaCategory);
            if (null != newTemplate) {
                TemplateProvider.getDefault().addTemplate(newTemplate);
                // update the template UUID of the current snippet
                m_snippet.getSettings().setTemplateUUID(newTemplate.getUUID());
                final String loc = TemplateProvider.getDefault().getDisplayLocation(newTemplate);
                m_templateLocation.setText(loc);
                validate();
            }
        });
        final JPanel templateInfoPanel = new JPanel(new BorderLayout());
        final TemplateProvider provider = TemplateProvider.getDefault();
        final String uuid = m_snippet.getSettings().getTemplateUUID();
        final RSnippetTemplate template = null != uuid ? provider.getTemplate(UUID.fromString(uuid)) : null;
        final String loc = null != template ? createTemplateLocationText(template) : "";
        m_templateLocation = new JLabel(loc);
        if (isPreview) {
            templateInfoPanel.add(m_templateLocation, BorderLayout.CENTER);
        } else {
            templateInfoPanel.add(addTemplateButton, BorderLayout.LINE_END);
        }
        templateInfoPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return templateInfoPanel;
    }

    /**
     * Create an empty, titled border.
     *
     * @param title Title of the border.
     * @return Such a new border.
     */
    protected Border createEmptyTitledBorder(final String title) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), title,
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
    }

    /**
     * Determines whether this component is enabled. An enabled component can respond to user input and generate events.
     *
     * @return <code>true</code> if the component is enabled, <code>false</code> otherwise
     */
    @Override
    public boolean isEnabled() {
        return m_isEnabled;
    }

    /**
     * Sets whether or not this component is enabled. A component that is enabled may respond to user input, while a
     * component that is not enabled cannot respond to user input.
     *
     * @param enabled true if this component should be enabled, false otherwise
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

    /**
     * Get names of objects currently defined in the workspace.
     *
     * @return array of names, never <code>null</code>
     */
    protected String[] rGetObjectNames() {
        REXP rexp;
        try {
            // we use the command queue to make sure commands are executed
            // strictly sequentially.
            rexp = m_commandQueue.putRScript("ls()", false).get();
            return (rexp != null) ? rexp.asStrings() : new String[]{};
        } catch (final Exception e) {
            m_consoleController.append("Warning: Could not get names of objects defined in workspace.", 1);
            return new String[]{};
        }
    }

    /**
     * Get classes of objects currently defined in the workspace.
     *
     * @return array of names, never <code>null</code>
     */
    protected String[] rGetObjectClasses() {
        REXP rexp;
        try {
            // we use the command queue to make sure commands are executed
            // strictly sequentially.
            rexp = m_commandQueue.putRScript("sapply(ls(),function(a)class(get(a,envir=globalenv()))[1])", false).get();
            return (rexp != null) ? rexp.asStrings() : new String[]{};
        } catch (final Exception e) {
            m_consoleController.append("Warning: Could not get classes of objects defined in workspace.", 1);
            return new String[]{};
        }
    }

    /**
     * Reinitialize with the given blueprint.
     *
     * @param template the template
     * @param flowVariables the flow variables at the input
     * @param spec the input spec
     */
    @Override
    public void applyTemplate(final RSnippetTemplate template, final DataTableSpec spec,
        final Map<String, FlowVariable> flowVariables) {
        // save and read settings to decouple objects.
        final NodeSettings settings = new NodeSettings(template.getUUID());
        template.getSnippetSettings().saveSettings(settings);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            settings.saveToXML(os);
            final NodeSettingsRO settingsro =
                NodeSettings.loadFromXML(new ByteArrayInputStream(os.toString("UTF-8").getBytes("UTF-8")));
            m_snippet.getSettings().loadSettings(settingsro);
        } catch (final Exception e) {
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
     * @param template the template
     * @return the template's location for display
     */
    private String createTemplateLocationText(final RSnippetTemplate template) {
        final TemplateProvider provider = TemplateProvider.getDefault();
        return provider.getDisplayLocation(template);
    }

    /**
     *
     * @return
     */
    public boolean onOpen() {
        m_closing = false;
        if (m_isInteractive) {
            try {
                m_console.setText("");
                m_objectBrowser.updateData(new String[0], new String[0]);

                m_snippetTextArea.requestFocus();
                m_snippetTextArea.requestFocusInWindow();

                // Create temporary image for the R plot output
                try {
                    m_tempDir = FileUtil.createTempDir("rtmp-dialog-");
                    m_imageFile = new File(m_tempDir, "rsnippet-plot.png");

                    // switch working directory to m_tempDir:
                    m_commandQueue.putRScript("setwd(\"" + m_tempDir.getAbsolutePath().replace('\\', '/') + "\")\n",
                        false);
                } catch (final IOException e) {
                    throw new RException("No PNG image file handle could be created - plot's won't work", e);
                }

                connectToR();

                // enable/disable buttons depending on R according to whether
                // initialization of RController succeeded
                final boolean enabled = m_controller.isInitialized();
                m_evalScriptButton.setEnabled(enabled);
                m_evalSelButton.setEnabled(enabled);
                m_resetWorkspace.setEnabled(enabled);
                m_showPlot.setEnabled(enabled);

            } catch (final RException e) {
                final StyledDocument doc = m_console.getStyledDocument();
                try {
                    doc.insertString(doc.getLength(), "R cannot be initialized.\n", m_console.getErrorStyle());

                    doc.insertString(doc.getLength(), e.getMessage(), m_console.getErrorStyle());
                } catch (final BadLocationException ex) {
                    // never happens
                    throw new RuntimeException(ex);
                }
            }
        }

        return true;
    }

    private void connectToR() throws RException {
        m_controller.initialize();

        m_consoleController.attachOutput(m_console);
        m_commandQueue.startExecutionThread(() -> {
            if (m_exec != null) {
                m_exec.getProgressMonitor().removeAllProgressListener();
            }

            m_exec = new ExecutionMonitor(new DefaultNodeProgressMonitor());
            m_exec.setProgress(0.0f); // for correct initial progress display
            m_progressPanel.startMonitoring(m_exec);
            return m_exec;
        }, true);

        // send data to R
        resetWorkspace();
    }

    public void onClose() {
        m_closing = true;
        if (m_previewFrame != null) {
            m_previewFrame.dispose();
            m_previewFrame = null;
        }
        if (m_tempDir != null) {
            // delete the temporary directory including its contents
            FileUtil.deleteRecursively(m_tempDir);
            m_imageFile = null;
            m_tempDir = null;
        }
        m_progressPanel.stopMonitoring();
        if (m_isInteractive && (m_controller != null)) {
            // Stop running tasks
            if (m_exec != null) {
                m_exec.getProgressMonitor().setExecuteCanceled();
            }

            detachConsoleController();
        }

        closeRController();
    }

    private void detachConsoleController() {
        if (m_consoleController != null && m_consoleController.isAttached(m_console)) {
            m_consoleController.cancel();
            m_consoleController.detach(m_console);
        }
    }

    private void closeRController() {
        if (m_controller != null && m_controller.isInitialized()) {
            try {
                m_controller.close();
            } catch (final RException e) {
                LOGGER.error("Failed to close Rserve connection", e);
            }
        }
    }

    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final RSnippetSettings s = m_snippet.getSettings();

        s.saveSettings(settings);
    }

    public void updateData(final ConfigRO settings, final PortObjectSpec[] specs,
        final Collection<FlowVariable> flowVariables) {
        m_snippet.getSettings().loadSettingsForDialog(settings);
        final DataTableSpec spec = m_tableInPort >= 0 ? (DataTableSpec)specs[m_tableInPort] : null;
        updateData(m_snippet.getSettings(), null, spec, flowVariables);
    }

    public void updateData(final ConfigRO settings, final PortObject[] input,
        final Collection<FlowVariable> flowVariables) {
        m_snippet.getSettings().loadSettingsForDialog(settings);
        final DataTableSpec spec = m_tableInPort >= 0 ? ((BufferedDataTable)input[m_tableInPort]).getSpec() : null;
        updateData(m_snippet.getSettings(), input, spec, flowVariables);
    }

    private void updateData(final RSnippetSettings settings, final PortObject[] input, final DataTableSpec spec,
        final Collection<FlowVariable> flowVariables) {
        ViewUtils.invokeAndWaitInEDT(() -> updateDataInternal(settings, input, spec, flowVariables));
    }

    protected void updateDataInternal(final RSnippetSettings settings, final PortObject[] input,
        final DataTableSpec spec, final Collection<FlowVariable> flowVariables) {
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
        final TemplateProvider provider = TemplateProvider.getDefault();
        final String uuid = m_snippet.getSettings().getTemplateUUID();
        final RSnippetTemplate template = null != uuid ? provider.getTemplate(UUID.fromString(uuid)) : null;
        final String loc = null != template ? createTemplateLocationText(template) : "";
        m_templateLocation.setText(loc);
    }

    /**
     * Returns the RSnippet.
     *
     * @return the RSnippet
     */
    public RSnippet getRSnippet() {
        return m_snippet;
    }

    private void evalScriptFragment(final String script) {
        // disable dialog compoments
        enableComponents(false);
        final String setupPlotInitCommand = setupPlotInitCommand();
        m_commandQueue.putRScript(setupPlotInitCommand, false);
        m_commandQueue.putRScript(script, true);
        final RCommand future = m_commandQueue.putRScript("dev.off()", false);

        // update the Panel when execution has finished.
        final Runnable checkIfDone = new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        future.get(50, TimeUnit.MILLISECONDS);
                    } catch (final TimeoutException e) {
                        SwingUtilities.invokeLater(this);
                        return;
                    }
                    if (m_commandQueue.isEmpty()) {
                        workspaceChanged();
                    }
                } catch (final Exception e) {
                }
            }
        };

        SwingUtilities.invokeLater(checkIfDone);
    }

    /**
     * Update the Panel according to changes in the R workspace.
     */
    public void workspaceChanged() {
        // enable dialog components
        enableComponents(true);
        ViewUtils.runOrInvokeLaterInEDT(() -> {
            final String[] objectNames = rGetObjectNames();
            if ((objectNames != null) && (objectNames.length > 0)) {
                final String[] objectClasses = rGetObjectClasses();
                m_objectBrowser.updateData(objectNames, objectClasses);
                if ((m_previewFrame != null) && m_previewFrame.isVisible()) {
                    showPlot();
                }
            }
        });
    }

    /**
     * Enables or disables the snippet text area, the eval, and the reset workspace buttons.
     *
     * @param enabled <code>True/False</code> to enable/disable the components
     */
    private void enableComponents(final boolean enabled) {
        ViewUtils.runOrInvokeLaterInEDT(() -> {
            m_evalScriptButton.setEnabled(enabled);
            m_evalSelButton.setEnabled(enabled);
            m_resetWorkspace.setEnabled(enabled);
        });
    }

}
