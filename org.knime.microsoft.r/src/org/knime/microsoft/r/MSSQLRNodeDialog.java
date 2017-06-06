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
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.microsoft.r;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetNodePanel;
import org.knime.r.RSnippetTemplate;
import org.knime.r.template.DefaultTemplateController;
import org.knime.r.template.TemplatesPanel;

/**
 * The dialog of the R nodes.
 *
 * @author Heiko Hofer
 */
public class MSSQLRNodeDialog extends DataAwareNodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MSSQLRNodeDialog.class);

    private static final String SNIPPET_TAB = "R Snippet";

    private static final String PNG_SETTINGS_TAB = "PNG Settings";

    private final MSSQLRNodeSettings m_settings;

    private final RSnippetNodePanel m_panel;

    private DefaultTemplateController m_templatesController;

    private final Class<?> m_templateMetaCategory;

    private final RSnippetNodeConfig m_config;

    private int m_tableInPort;

    private JTextField m_imgWidth;

    private JTextField m_imgHeight;

    private JTextField m_imgResolution;

    private JTextField m_textPointSize;

    private JTextField m_imgBackgroundColor;

    /**
     * Create a new Dialog.
     *
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     */
    protected MSSQLRNodeDialog(final Class<?> templateMetaCategory, final RSnippetNodeConfig config) {
        m_settings = new MSSQLRNodeSettings();
        m_templateMetaCategory = templateMetaCategory;
        m_config = config;
        m_tableInPort = -1;
        int i = 0;
        for (final PortType portType : m_config.getInPortTypes()) {
            if (portType.equals(BufferedDataTable.TYPE)) {
                m_tableInPort = i;
            }
            i++;
        }

        m_panel = new RSnippetNodePanel(templateMetaCategory, m_config, false, true) {

            /**
             *
             */
            private static final long serialVersionUID = -1154071447343773118L;

            @Override
            public void applyTemplate(final RSnippetTemplate template, final DataTableSpec spec,
                final Map<String, FlowVariable> flowVariables) {
                super.applyTemplate(template, spec, flowVariables);
                setSelected(SNIPPET_TAB);
            }

            @Override
            protected Dimension getPreviewImageDimensions() {
                return new Dimension(0, 0);
            }

        };

        addTab(SNIPPET_TAB, m_panel);
        addTab(PNG_SETTINGS_TAB, createPNGSettingsPanel());
        // The preview does not have the templates tab
        addTab("Templates", createTemplatesPanel());

        m_panel.setPreferredSize(new Dimension(800, 600));
    }

    private JPanel createPNGSettingsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        new Insets(3, 8, 3, 8);
        new Insets(3, 0, 3, 8);
        final Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        new Insets(11, 0, 3, 8);

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 1;
        final JPanel imagePanel = createImagePanel();
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));
        p.add(imagePanel, c);

        c.gridy++;
        final JPanel appearancePanel = createAppearancePanel();
        appearancePanel.setBorder(BorderFactory.createTitledBorder("Appearance"));
        p.add(appearancePanel, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);
        return p;
    }

    private JPanel createImagePanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        final Insets leftInsets = new Insets(3, 0, 3, 8);
        final Insets rightInsets = new Insets(3, 0, 3, 0);
        final Insets leftCategoryInsets = new Insets(0, 0, 3, 8);
        final Insets rightCategoryInsets = new Insets(0, 0, 3, 0);

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Width (in Pixel):"), c);
        c.gridx = 1;
        c.insets = rightCategoryInsets;
        c.weightx = 1;
        m_imgWidth = new JTextField();
        p.add(m_imgWidth, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Height (in Pixel):"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_imgHeight = new JTextField();
        p.add(m_imgHeight, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Resolution (dpi):"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_imgResolution = new JTextField();
        p.add(m_imgResolution, c);

        return p;
    }

    private JPanel createAppearancePanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        final Insets leftInsets = new Insets(3, 0, 3, 8);
        final Insets rightInsets = new Insets(3, 0, 3, 0);
        new Insets(0, 0, 3, 8);
        new Insets(0, 0, 3, 0);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Text point size:"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_textPointSize = new JTextField();
        p.add(m_textPointSize, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Background color:"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_imgBackgroundColor = new JTextField();
        p.add(m_imgBackgroundColor, c);

        return p;
    }

    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        final RSnippetNodePanel preview = new RSnippetNodePanel(m_templateMetaCategory, m_config, true, false);

        m_templatesController = new DefaultTemplateController(m_panel, preview);
        final TemplatesPanel templatesPanel =
            new TemplatesPanel(Collections.<Class<?>> singleton(m_templateMetaCategory), m_templatesController);
        return templatesPanel;
    }

    @Override
    public boolean closeOnESC() {
        // do not close on ESC, since ESC is used to close autocomplete popups
        // in the snippets textarea.
        return false;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if(specs[0] == null) {
            return;
        }
        assert specs[0] instanceof DatabaseConnectionPortObjectSpec;

        try {
            final DatabaseConnectionSettings s = ((DatabaseConnectionPortObjectSpec)specs[0]).getConnectionSettings(getCredentialsProvider());
            ((MSSQLRNodeModel.RNodeConfig)m_config).setDatabaseSettings(s);
        } catch (InvalidSettingsException e) {
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
        throws NotConfigurableException {
        if(input[0] == null) {
            return;
        }
        assert input[0] instanceof DatabaseConnectionPortObject;

        try {
            final DatabaseConnectionSettings s = ((DatabaseConnectionPortObject)input[0]).getSpec().getConnectionSettings(getCredentialsProvider());
            ((MSSQLRNodeModel.RNodeConfig)m_config).setDatabaseSettings(s);
        } catch (InvalidSettingsException e) {
        }
    }

    @Override
    public void onOpen() {
        ViewUtils.invokeAndWaitInEDT(() -> m_panel.onOpen());
    }

    @Override
    public void onClose() {
        ViewUtils.invokeAndWaitInEDT(() -> m_panel.onClose());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
    }

}
