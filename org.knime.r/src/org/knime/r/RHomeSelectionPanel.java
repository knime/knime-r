/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Mar 17, 2020 (benjamin): created
 */
package org.knime.r;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.RBinUtil.InvalidRHomeException;
import org.knime.ext.r.bin.preferences.DefaultRPreferenceProvider;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.ext.r.bin.preferences.RPreferenceProvider;
import org.knime.workbench.core.util.ImageRepository;

/**
 * A panel which shows a checkbox and file chooser for selecting the R home.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class RHomeSelectionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Icon ERROR_ICON = new ImageIcon(ImageRepository.SharedImages.Error.getUrl());

    private static final Icon WARNING_ICON = new ImageIcon(ImageRepository.SharedImages.Warning.getUrl());

    /** Width of the R home error label */
    private final int m_rHomeErrorWidth;

    private final FlowVariableModel m_rHomeModel;

    private final JCheckBox m_overwriteRHome;

    private final FilesHistoryPanel m_rHome;

    private final JLabel m_rHomeError;

    private boolean m_hasError;

    RHomeSelectionPanel(final int rHomeErrorWidth, final FlowVariableModel rHomeModel) {
        super(new GridBagLayout());
        m_rHomeErrorWidth = rHomeErrorWidth;
        m_rHomeModel = rHomeModel;

        setBorder(new TitledBorder("Path to R home"));

        final Insets insets = new Insets(5, 5, 0, 5);
        final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.BASELINE,
            GridBagConstraints.HORIZONTAL, insets, 0, 0);

        // Use a separate R home
        m_overwriteRHome =
            new JCheckBox("Overwrite default path to R home (replaces the path in the application preferences)");
        m_overwriteRHome.addActionListener(e -> overwriteRHomeChanged());
        gbc.gridwidth = 2;
        add(m_overwriteRHome, gbc);
        gbc.gridy++;

        // R home selection
        m_rHome = new FilesHistoryPanel(m_rHomeModel, "r_home_path", LocationValidation.None);
        m_rHome.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_rHome.setDialogType(JFileChooser.OPEN_DIALOG);
        m_rHome.addChangeListener(e -> rHomeChanged());
        add(m_rHome, gbc);
        gbc.gridy++;

        // R home error
        m_rHomeError = new JLabel();
        m_rHomeError.setVerticalTextPosition(SwingConstants.TOP);
        gbc.insets = new Insets(5, 10, 5, 10);
        add(m_rHomeError, gbc);
        gbc.gridy++;
    }

    /** Get the current R preferences. According to the flow variable if it is set */
    RPreferenceProvider getRPreferenceProvider() {
        if (m_overwriteRHome.isSelected()) {
            final Optional<FlowVariable> rHome = m_rHomeModel.getVariableValue();
            if (rHome.isPresent()) {
                return new DefaultRPreferenceProvider(rHome.get().getStringValue());
            } else {
                return new DefaultRPreferenceProvider(m_rHome.getSelectedFile());
            }
        } else {
            return RPreferenceInitializer.getRProvider();
        }
    }

    /** Load the configured R home from the snippet settings */
    void loadSettingsFrom(final RSnippetSettings settings) {
        m_overwriteRHome.setSelected(settings.isOverwriteRHome());
        m_rHome.setSelectedFile(settings.getRHomePath());
        overwriteRHomeChanged();
    }

    /** Save the configured R home to the snippet settings */
    void saveSettingsTo(final RSnippetSettings settings) {
        settings.setOverwriteRHome(m_overwriteRHome.isSelected());
        settings.setRHomePath(m_rHome.getSelectedFile());
    }

    /** Checks if the configured R home is valid and throws an exception if it is not valid */
    void checkSettings() throws InvalidSettingsException {
        if (m_hasError) {
            throw new InvalidSettingsException(
                "The R home configuration is invalid. Please select a valid R home on the \"Advanced\" tab.");
        }
    }

    /** Called when the separate R home checkbox is clicked. */
    private void overwriteRHomeChanged() {
        boolean enabled = m_overwriteRHome.isSelected();
        m_rHome.setEnabled(enabled);
        m_rHomeError.setEnabled(enabled);
        rHomeChanged();
    }

    /**
     * Called if the R home has changed to check the new R home and display errors. Note that this does not trigger an
     * update of the snippet.
     */
    private void rHomeChanged() {
        final RPreferenceProvider pref = getRPreferenceProvider();
        try {
            RBinUtil.checkRHome(pref.getRHome());
        } catch (final InvalidRHomeException e) {
            // The R home is invalid: Display the message
            m_hasError = true;
            setRHomeError(e.getMessage(), true);
            return;
        }
        // The R home is valid: Delete the old message
        m_hasError = false;
        m_rHomeError.setIcon(null);
        m_rHomeError.setText("");
    }

    /** Set the R home error */
    private void setRHomeError(final String message, final boolean error) {
        final Icon icon = error ? ERROR_ICON : WARNING_ICON;
        m_rHomeError.setIcon(icon);
        m_rHomeError
            .setText(String.format("<html><div style=\\\"width:%dpx;\\\">%s</div></html>", m_rHomeErrorWidth, message));
    }
}
