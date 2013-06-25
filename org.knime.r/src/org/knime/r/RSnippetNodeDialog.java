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

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.template.DefaultTemplateController;
import org.knime.r.template.TemplatesPanel;

/**
 * The dialog of the R nodes.
 *
 * @author Heiko Hofer
 */
public class RSnippetNodeDialog extends DataAwareNodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            RSnippetNodeDialog.class);

    private static final String SNIPPET_TAB = "R Snippet";


	private RSnippetNodePanel m_panel;
	private DefaultTemplateController m_templatesController;

	private Class m_templateMetaCategory;
	private RSnippetNodeConfig m_config;
	private int m_tableInPort;

	private PortObject[] m_input;

    /**
     * Create a new Dialog.
     * @param templateMetaCategory the meta category used in the templates
     * tab or to create templates
     */
    @SuppressWarnings("rawtypes")
    protected RSnippetNodeDialog(final Class templateMetaCategory, final RSnippetNodeConfig config) {
    	m_templateMetaCategory = templateMetaCategory;
    	m_config = config;
    	m_tableInPort = -1;
    	int i = 0;
    	for (PortType portType : m_config.getInPortTypes()) {
    		if (portType.equals(BufferedDataTable.TYPE)) {
    			m_tableInPort = i;
        	}
    		i++;
    	}

        m_panel = new RSnippetNodePanel(templateMetaCategory, m_config, false, true) {

        	@Override
			public void applyTemplate(final RSnippetTemplate template,
                    final DataTableSpec spec,
                    final Map<String, FlowVariable> flowVariables) {
        		super.applyTemplate(template, spec, flowVariables);
        		setSelected(SNIPPET_TAB);
        	}

        };

        addTab(SNIPPET_TAB, m_panel);
        // The preview does not have the templates tab
        addTab("Templates", createTemplatesPanel());

        m_panel.setPreferredSize(new Dimension(800, 600));
    }

    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        RSnippetNodePanel preview = new RSnippetNodePanel(m_templateMetaCategory, m_config, true, false);

        m_templatesController = new DefaultTemplateController(
                m_panel, preview);
        TemplatesPanel templatesPanel = new TemplatesPanel(
                Collections.singleton(m_templateMetaCategory),
                m_templatesController);
        return templatesPanel;
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
    		final PortObjectSpec[] specs) throws NotConfigurableException {
    	DataTableSpec spec = m_tableInPort >= 0 ? (DataTableSpec)specs[m_tableInPort] : null;
    	m_input = null;
        m_panel.updateData(settings, specs, getAvailableFlowVariables().values());

        m_templatesController.setDataTableSpec(spec);
        m_templatesController.setFlowVariables(getAvailableFlowVariables());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
    		final PortObject[] input) throws NotConfigurableException {
    	DataTableSpec spec = m_tableInPort >= 0 ? ((BufferedDataTable)input[m_tableInPort]).getSpec() : null;
    	m_input = input;
        m_panel.updateData(settings, input, getAvailableFlowVariables().values());

        m_templatesController.setDataTableSpec(spec);
        m_templatesController.setFlowVariables(getAvailableFlowVariables());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {
        final ValueReport<Boolean> report = m_panel.onOpen();
        if (report.hasErrors()) {
        	final Component parent = m_panel;
        	ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(parent, ValueReport.joinString(report.getErrors(), "\n"));
				}
			});
        }
    }



	@Override
    public void onClose() {
		m_panel.onClose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_panel.saveSettingsTo(settings);
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
