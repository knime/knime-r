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
import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.RSnippetNodeConfig;
import org.knime.r.RSnippetTemplate;
import org.knime.r.template.DefaultTemplateController;
import org.knime.r.template.TemplatesPanel;

/**
 * The dialog of the R nodes.
 *
 * @author Heiko Hofer
 */
public class DeployRToMSSQLNodeDialog extends DataAwareNodeDialogPane {
    // private static final NodeLogger LOGGER = NodeLogger.getLogger(DeployRToMSSQLNodeDialog.class);

    private static final String SNIPPET_TAB = "R Snippet";

    private final DeployRToMSSQLNodeSettings m_settings;

    private final SimpleRSnippetNodePanel m_panel;

    private DefaultTemplateController<SimpleRSnippetNodePanel> m_templatesController;

    private final Class<?> m_templateMetaCategory;

    private final RSnippetNodeConfig m_config;

    /**
     * Create a new Dialog.
     *
     * @param templateMetaCategory the meta category used in the templates tab or to create templates
     * @param config R Snippet node config to customize the dialog and node model
     */
    protected DeployRToMSSQLNodeDialog(final Class<?> templateMetaCategory, final RSnippetNodeConfig config) {
        m_settings = new DeployRToMSSQLNodeSettings();
        m_templateMetaCategory = templateMetaCategory;
        m_config = config;

        m_panel = new SimpleRSnippetNodePanel(templateMetaCategory, m_config, m_settings, false, false) {

            private static final long serialVersionUID = -1154071447343773118L;

            @Override
            public void applyTemplate(final RSnippetTemplate template, final DataTableSpec spec,
                final Map<String, FlowVariable> flowVariables) {
                super.applyTemplate(template, spec, flowVariables);
                setSelected(SNIPPET_TAB);
            }
        };

        addTab(SNIPPET_TAB, m_panel);
        addTab("Templates", createTemplatesPanel());

        m_panel.setPreferredSize(new Dimension(800, 600));
    }

    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        final SimpleRSnippetNodePanel preview =
            new SimpleRSnippetNodePanel(m_templateMetaCategory, m_config, m_settings, true, false);

        m_templatesController = new DefaultTemplateController<>(m_panel, preview);
        return new TemplatesPanel(Collections.singleton(m_templateMetaCategory), m_templatesController);
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
        m_panel.updateData(settings, specs, getAvailableFlowVariables().values());
        m_settings.loadSettingsForDialog(settings);
        m_panel.getSnippetSettings().loadSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
        throws NotConfigurableException {
        m_panel.updateData(settings, input, getAvailableFlowVariables().values());
        m_settings.loadSettingsForDialog(settings);
        m_panel.getSnippetSettings().loadSettingsForDialog(settings);
    }

    @Override
    public void onOpen() {
        ViewUtils.invokeAndWaitInEDT(() -> m_panel.onOpen());
    }

    @Override
    public void onClose() {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.saveSettingsTo(settings);
        m_panel.getSnippetSettings().saveSettings(settings);
    }

}
