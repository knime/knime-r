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
package org.knime.microsoft.r;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.RSnippet;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetNodePanel;
import org.knime.r.RSnippetSettings;
import org.knime.r.RSnippetTemplate;
import org.knime.r.template.AddTemplateDialog;
import org.knime.r.template.TemplateProvider;
import org.knime.r.template.TemplateReceiver;
import org.knime.r.ui.RColumnList;
import org.knime.r.ui.RFlowVariableList;
import org.knime.r.ui.RSnippetTextArea;

/**
 * The dialog component for RSnippet-Nodes.
 *
 * Simple version of {@link RSnippetNodePanel} without the console, column list and workspace view, as these do not
 * simply apply to MSSQL R.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
class SimpleRSnippetNodePanel extends JPanel implements TemplateReceiver {

    /** Generated serialVersionUID */
    private static final long serialVersionUID = 2286323699400964363L;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RunRInMSSQLNodeDialog.class);

    private RSnippetTextArea m_snippetTextArea;

    /** Component with a list of all input flow variables. */
    protected RFlowVariableList m_flowVarsList;

    private RColumnList m_dbColumnsList = new RColumnList();

    private final RSnippet m_snippet;

    private final RunRInMSSQLNodeSettings m_settings;

    private final boolean m_isInteractive;

    /** The templates category for templates viewed or edited by this dialog. */
    protected Class<?> m_templateMetaCategory;

    private JLabel m_templateLocation;

    private final RSnippetNodeConfig m_config;

    private final JTextField m_sqlOutTableNameTextField = new JTextField();

    private JCheckBox m_overwriteCheckBox = new JCheckBox("Overwrite");

    /**
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     * @param config
     * @param settings Node settings to write to from the panel (e.g. for SQL output table name)
     * @param isPreview if this is a preview used for showing templates.
     * @param isInteractive
     */
    public SimpleRSnippetNodePanel(final Class<?> templateMetaCategory, final RSnippetNodeConfig config,
        final RunRInMSSQLNodeSettings settings, final boolean isPreview, final boolean isInteractive) {
        super(new BorderLayout());
        m_config = config;
        m_settings = settings;

        m_templateMetaCategory = templateMetaCategory;

        m_isInteractive = isPreview ? false : isInteractive;

        m_snippet = new RSnippet();
        final JPanel panel = createPanel(isPreview, m_isInteractive);
        m_flowVarsList.install(m_snippetTextArea);

        setEnabled(!isPreview);
        panel.setPreferredSize(new Dimension(1280, 720));

        m_sqlOutTableNameTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                setSettings();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                setSettings();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                setSettings();
            }

            private void setSettings() {
                settings.setOutputTableName(m_sqlOutTableNameTextField.getText());
            }
        });

        // Change the setting value according to checkbox changes
        m_overwriteCheckBox.addChangeListener(e -> {
            settings.setOverwriteOutputTable(m_overwriteCheckBox.isSelected());
        });
        // Change the check box according to settings changes.
        // This is not an infinite loop thanks to this event only being fired on
        // effective changes.
        settings.overwriteOutputTableModel().addChangeListener(e -> {
            m_overwriteCheckBox.setSelected(settings.getOverwriteOutputTable());
        });

        m_dbColumnsList.install(m_snippetTextArea);
    }

    private JPanel createPanel(final boolean isPreview, final boolean isInteractive) {
        final JComponent snippet = createSnippetPanel();
        final JPanel snippetPanel = new JPanel(new BorderLayout());
        snippetPanel.add(snippet, BorderLayout.CENTER);

        final JComponent colsAndVars = createColsAndVarsPanel();

        final JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplitPane.setLeftComponent(colsAndVars);
        leftSplitPane.setRightComponent(snippetPanel);
        leftSplitPane.setDividerLocation(200);

        add(leftSplitPane, BorderLayout.CENTER);
        final JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
        add(templateInfoPanel, BorderLayout.NORTH);
        return this;
    }

    /**
     * Create the panel with the snippet.
     */
    private JComponent createSnippetPanel() {
        m_snippetTextArea = new RSnippetTextArea(m_snippet);

        final JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
        snippetScroller.setBorder(createEmptyTitledBorder("R Script"));

        final JPanel snippet = new JPanel(new BorderLayout());
        snippet.add(snippetScroller, BorderLayout.CENTER);

        final ErrorStrip es = new ErrorStrip(m_snippetTextArea);
        snippet.add(es, BorderLayout.LINE_END);

        return snippet;
    }

    /**
     * @return Snippet settings of the dialogs {@link RSnippet}.
     */
    public RSnippetSettings getSnippetSettings() {
        return m_snippet.getSettings();
    }

    /**
     * The panel at the left with the column and variables at the input. Override this method when the columns or
     * variables should not be displayed.
     *
     * @return the panel at the left with the column and variables at the input.
     */
    protected JComponent createColsAndVarsPanel() {
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        final JPanel columnPanel = new JPanel(new BorderLayout());
        final JScrollPane columnScroller = new JScrollPane(m_dbColumnsList);
        columnScroller.setBorder(createEmptyTitledBorder("Input Columns"));
        columnPanel.add(columnScroller, BorderLayout.CENTER);


        m_flowVarsList = new RFlowVariableList();
        final JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(createEmptyTitledBorder("Flow Variable List"));

        split.setTopComponent(columnPanel);
        split.setBottomComponent(flowVarScroller);
        split.setDividerLocation(0.5);

        return split;
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
            final JPanel tableNamePanel = new JPanel(new FlowLayout());
            tableNamePanel.add(new JLabel("SQL Output Table Name: "));
            tableNamePanel.add(m_sqlOutTableNameTextField);
            tableNamePanel.add(new JLabel("(knime.out)"));
            tableNamePanel.add(m_overwriteCheckBox);

            final Dimension d = m_sqlOutTableNameTextField.getPreferredSize();
            d.width = 150;
            m_sqlOutTableNameTextField.setPreferredSize(d);

            templateInfoPanel.add(tableNamePanel, BorderLayout.LINE_START);
            //templateInfoPanel.add(ViewUtils.getInFlowLayout(addTemplateButton), BorderLayout.LINE_END);
        }
        templateInfoPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return templateInfoPanel;
    }

    /* Create an empty, titled border. */
    private Border createEmptyTitledBorder(final String title) {
        return BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), title,
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
    }

    /**
     * Sets whether or not this component is enabled. A component that is enabled may respond to user input, while a
     * component that is not enabled cannot respond to user input.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(final boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            m_flowVarsList.setEnabled(enabled);
            m_snippetTextArea.setEnabled(enabled);
            m_dbColumnsList.setEnabled(enabled);
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
     * Call this function in {@link NodeDialogPane#onOpen()}
     *
     * @return `true`
     */
    public boolean onOpen() {
        m_snippetTextArea.requestFocus();
        m_snippetTextArea.requestFocusInWindow();

        m_sqlOutTableNameTextField.setText(m_settings.getOutputTableName());

        return true;
    }

    /**
     * Update pane contents to match given data
     *
     * @param settings R Snippet settings to update the panel with
     * @param specs Input port specs
     * @param flowVariables Flow variables
     */
    public void updateData(final ConfigRO settings, final PortObjectSpec[] specs,
        final Collection<FlowVariable> flowVariables) {
        m_snippet.getSettings().loadSettingsForDialog(settings);
        final DataTableSpec spec = specs[1] == null ? null : ((DatabasePortObjectSpec)specs[1]).getDataTableSpec();
        updateData(m_snippet.getSettings(), null, spec, flowVariables);
    }

    /**
     * Update pane contents to match given data
     *
     * @param settings R Snippet settings to update the panel with
     * @param input Input data at input ports
     * @param flowVariables Flow variables
     */
    public void updateData(final ConfigRO settings, final PortObject[] input,
        final Collection<FlowVariable> flowVariables) {
        m_snippet.getSettings().loadSettingsForDialog(settings);
        final DataTableSpec spec = input[1] == null ? null : ((DatabasePortObject)input[1]).getSpec().getDataTableSpec();
        updateData(m_snippet.getSettings(), input, spec, flowVariables);
    }

    /**
     * Update pane contents to match given data
     *
     * @param settings R Snippet settings to update the panel with
     * @param input Input data at input ports
     * @param spec Input Table specification
     * @param flowVariables Flow variables
     */
    private void updateData(final RSnippetSettings settings, final PortObject[] input, final DataTableSpec spec,
        final Collection<FlowVariable> flowVariables) {
        ViewUtils.invokeAndWaitInEDT(() -> updateDataInternal(settings, input, spec, flowVariables));
    }

    /**
     * Update pane contents to match given data
     *
     * @param settings R Snippet settings to update the panel with
     * @param input Input data at input ports
     * @param spec Input Table specification
     * @param flowVariables Flow variables
     */
    protected void updateDataInternal(final RSnippetSettings settings, final PortObject[] input,
        final DataTableSpec spec, final Collection<FlowVariable> flowVariables) {
        m_snippet.getSettings().loadSettings(settings);

        m_flowVarsList.setFlowVariables(flowVariables);

        m_dbColumnsList.setSpec(spec);

        // update template info panel
        final TemplateProvider provider = TemplateProvider.getDefault();
        final String uuid = m_snippet.getSettings().getTemplateUUID();
        final RSnippetTemplate template = null != uuid ? provider.getTemplate(UUID.fromString(uuid)) : null;
        final String loc = null != template ? createTemplateLocationText(template) : "";
        m_templateLocation.setText(loc);
    }
}
