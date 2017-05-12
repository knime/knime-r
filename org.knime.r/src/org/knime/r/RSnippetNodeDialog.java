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
package org.knime.r;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

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
 * @author Jonathan Hale
 */
public class RSnippetNodeDialog extends DataAwareNodeDialogPane {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RSnippetNodeDialog.class);

	private static final String SNIPPET_TAB = "R Snippet";

	private final RSnippetNodePanel m_panel;
	private DefaultTemplateController m_templatesController;

	private final Class<?> m_templateMetaCategory;
	private final RSnippetNodeConfig m_config;
	private int m_tableInPort;
	private int m_tableOutPort;

	private JCheckBox m_outNonNumbersAsMissing;
	private JCheckBox m_sendRowNames;
	private JComboBox<String> m_knimeInType;
	private JFormattedTextField m_sendBatchSize;

	/**
	 * Create a new Dialog.
	 *
	 * @param templateMetaCategory
	 *            the meta category used in the templates tab or to create
	 *            templates
	 */
	protected RSnippetNodeDialog(final Class<?> templateMetaCategory, final RSnippetNodeConfig config) {
		m_templateMetaCategory = templateMetaCategory;
		m_config = config;
		m_tableInPort = -1;
		int i = 0;
		for (final PortType portType : m_config.getInPortTypes()) {
			if (portType.equals(BufferedDataTable.TYPE)) {
				m_tableInPort = i;
			}
			++i;
		}

		m_tableOutPort = -1;
		i = 0;
		for (final PortType portType : m_config.getOutPortTypes()) {
			if (portType.equals(BufferedDataTable.TYPE)) {
				m_tableOutPort = i;
			}
			++i;
		}

		m_panel = new RSnippetNodePanel(templateMetaCategory, m_config, false, true) {

			/**
			 *
			 */
			private static final long serialVersionUID = 6934850660800321248L;

			@Override
			public void applyTemplate(final RSnippetTemplate template, final DataTableSpec spec,
					final Map<String, FlowVariable> flowVariables) {
				super.applyTemplate(template, spec, flowVariables);
				setSelected(SNIPPET_TAB);
			}

		};

		addTab(SNIPPET_TAB, m_panel);
		// The preview does not have the templates tab
		addTab("Templates", createTemplatesPanel());

		if (m_tableInPort >= 0 || m_tableOutPort >= 0) {
			addTab("Advanced", createAdvancedPanel());
		}
		m_panel.setPreferredSize(new Dimension(1280, 720));
	}

	/** Create the templates tab. */
	private JPanel createTemplatesPanel() {
		final RSnippetNodePanel preview = new RSnippetNodePanel(m_templateMetaCategory, m_config, true, false);

		m_templatesController = new DefaultTemplateController(m_panel, preview);
		final TemplatesPanel templatesPanel = new TemplatesPanel(
				Collections.<Class<?>> singleton(m_templateMetaCategory), m_templatesController);
		return templatesPanel;
	}

	private JPanel createAdvancedPanel() {
		final Insets insets = new Insets(5, 5, 0, 5);
		int y = 0;
		final GridBagConstraints gbc_nonNums = new GridBagConstraints(0, y, 1, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		++y;
		final GridBagConstraints gbc_sendRowNames = new GridBagConstraints(0, y, 1, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		++y;
		final GridBagConstraints gbc_knimeInTypeLbl = new GridBagConstraints(0, y, 1, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		final GridBagConstraints gbc_knimeInType = new GridBagConstraints(1, y, 2, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		++y;
		final GridBagConstraints gbc_sendBatchSizeLbl = new GridBagConstraints(0, y, 1, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		final GridBagConstraints gbc_sendBatchSize = new GridBagConstraints(1, y, 1, 1, 1, 0, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);
		++y;
		final GridBagConstraints gbc_filler = new GridBagConstraints(0, y, 1, 1, 1, 1, GridBagConstraints.BASELINE,
				GridBagConstraints.HORIZONTAL, insets, 0, 0);

		m_outNonNumbersAsMissing = new JCheckBox("Treat NaN, Inf and -Inf as missing values in the output table.");
		m_outNonNumbersAsMissing.setToolTipText("Check for backwards compatibility with pre 2.10 releases.");
		m_outNonNumbersAsMissing.setEnabled(m_tableOutPort >= 0);

		m_sendRowNames = new JCheckBox("Send row names of input table.");
		m_sendRowNames.setToolTipText("Disabling sending row names can improve performance with very large tables.");
		m_sendRowNames.setEnabled(m_tableInPort >= 0);

		m_knimeInType = new JComboBox<>(new String[]{"data.frame", "data.table (experimental!)"});
		m_knimeInType.setToolTipText("R type for knime.in. \"data.table\" requires the \"data.table\" package.");
		m_knimeInType.setEnabled(m_tableInPort >= 0);

		final NumberFormat fmt = NumberFormat.getNumberInstance();
		final NumberFormatter formatter = new NumberFormatter(fmt);
		formatter.setMinimum(1);
		formatter.setMaximum(1000000);
		formatter.setValueClass(Integer.class);
		m_sendBatchSize = new JFormattedTextField(formatter);
		m_sendBatchSize.setToolTipText("Number of rows to send to R per batch. This amount of rows will be kept in memory on KNIME side.");
		m_sendBatchSize.setEnabled(m_tableInPort >= 0);

		final JPanel p = new JPanel(new GridBagLayout());
		p.add(m_outNonNumbersAsMissing, gbc_nonNums);

		p.add(m_sendRowNames, gbc_sendRowNames);

		p.add(new JLabel("Type of \"knime.in\" variable."), gbc_knimeInTypeLbl);
		p.add(m_knimeInType, gbc_knimeInType);

		p.add(new JLabel("Number of rows to send to R per batch"), gbc_sendBatchSizeLbl);
		p.add(m_sendBatchSize, gbc_sendBatchSize);

		p.add(new JPanel(), gbc_filler); // Panel to fill up remaining space
		return p;
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
		final DataTableSpec spec = m_tableInPort >= 0 ? (DataTableSpec) specs[m_tableInPort] : null;
		m_panel.updateData(settings, specs, getAvailableFlowVariables().values());
		final RSnippetSettings s = new RSnippetSettings();
		s.loadSettingsForDialog(settings);

		if (m_tableOutPort >= 0) {
			m_outNonNumbersAsMissing.setSelected(s.getOutNonNumbersAsMissing());
		}

		if (m_tableInPort >= 0) {
			m_sendBatchSize.setValue(new Integer(s.getSendBatchSize()));

			final String type = s.getKnimeInType();
			m_knimeInType.setSelectedIndex(type.equals("data.table") ? 1 : 0);

			m_sendRowNames.setSelected(s.getSendRowNames());
		}

		m_templatesController.setDataTableSpec(spec);
		m_templatesController.setFlowVariables(getAvailableFlowVariables());
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
			throws NotConfigurableException {
		final DataTableSpec spec = m_tableInPort >= 0 ? ((BufferedDataTable) input[m_tableInPort]).getSpec() : null;
		m_panel.updateData(settings, input, getAvailableFlowVariables().values());

		final RSnippetSettings s = new RSnippetSettings();
		s.loadSettingsForDialog(settings);
		if (m_tableOutPort >= 0) {
			m_outNonNumbersAsMissing.setSelected(s.getOutNonNumbersAsMissing());
		}
		if (m_tableInPort >= 0) {
			m_sendRowNames.setSelected(s.getSendRowNames());

			m_sendBatchSize.setValue(new Integer(s.getSendBatchSize()));

			final String type = s.getKnimeInType();
			m_knimeInType.setSelectedIndex(type.equals("data.table") ? 1 : 0);
		}

		m_templatesController.setDataTableSpec(spec);
		m_templatesController.setFlowVariables(getAvailableFlowVariables());

	}

	@Override
	public void onOpen() {
		ViewUtils.invokeAndWaitInEDT(() -> {
			m_panel.onOpen();
		});
	}

	@Override
	public void onClose() {
		ViewUtils.invokeAndWaitInEDT(() -> {
			m_panel.onClose();
		});
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		final RSnippetSettings s = m_panel.getRSnippet().getSettings();

		if (m_tableOutPort >= 0) {
			s.setOutNonNumbersAsMissing(m_outNonNumbersAsMissing.isSelected());
		}
		if (m_tableInPort >= 0) {
			s.setSendRowNames(m_sendRowNames.isSelected());
			s.setSendBatchSize((Integer)m_sendBatchSize.getValue());
			s.setKnimeInType(m_knimeInType.getSelectedIndex() == 0 ? "data.frame" : "data.table");
		}

		m_panel.saveSettingsTo(settings);
	}

}
