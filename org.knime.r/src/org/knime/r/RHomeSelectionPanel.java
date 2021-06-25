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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.tuple.Pair;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.SwingWorkerWithContext;
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

    private static final Icon PROCESS_ICON = new ImageIcon(ImageRepository.SharedImages.Busy.getUrl());

    /** Width of the R home error label */
    private final int m_rHomeErrorWidth;

    private final FlowVariableModel m_rHomePathModel;

    private final SettingsModelString m_rCondaVariableNameModel;

    private final JCheckBox m_overwriteRHome;

    private final FilesHistoryPanel m_rHomePath;

    private final DialogComponentFlowVariableNameSelection2 m_rHomeConda;

    private final Supplier<Map<String, FlowVariable>> m_condaVariablesSupplier;

    private final JRadioButton m_pathSelection;

    private final JRadioButton m_condaSelection;

    private final ChangeListener m_rHomeChangeListener;

    private final JLabel m_rHomeError;

    private boolean m_hasError;

    private REnvChecker m_rEnvChecker = null;

    RHomeSelectionPanel(final int rHomeErrorWidth, final FlowVariableModel rHomeModel,
        final SettingsModelString rCondaVariableName,
        final Supplier<Map<String, FlowVariable>> condaVariablesSupplier) {
        super(new GridBagLayout());
        m_rHomeErrorWidth = rHomeErrorWidth;
        m_rHomePathModel = rHomeModel;
        m_rCondaVariableNameModel = rCondaVariableName;
        m_condaVariablesSupplier = condaVariablesSupplier;

        setBorder(new TitledBorder("Path to R home"));

        final Insets insets = new Insets(5, 5, 0, 5);
        final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE,
            GridBagConstraints.HORIZONTAL, insets, 0, 0);

        // Use a separate R home
        m_overwriteRHome =
            new JCheckBox("Overwrite default path to R home (replaces the path in the application preferences)");
        m_overwriteRHome.addActionListener(e -> overwriteRHomeChanged());
        gbc.gridwidth = 2;
        add(m_overwriteRHome, gbc);
        gbc.gridy++;

        //
        final var group = new ButtonGroup();
        m_pathSelection = new JRadioButton("Specify path to R home");
        m_condaSelection = new JRadioButton("Use conda environment to find R home");
        group.add(m_pathSelection);
        add(m_pathSelection, gbc);
        gbc.gridy++;

        m_rHomeChangeListener = e -> rHomeChanged();
        // R home selection
        m_rHomePath = new FilesHistoryPanel(m_rHomePathModel, "r_home_path", LocationValidation.None);
        m_rHomePath.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_rHomePath.setDialogType(JFileChooser.OPEN_DIALOG);
        m_rHomePath.addChangeListener(m_rHomeChangeListener);
        add(m_rHomePath, gbc);
        gbc.gridy++;

        group.add(m_condaSelection);
        add(m_condaSelection, gbc);
        gbc.gridy++;

        m_rHomeConda = new DialogComponentFlowVariableNameSelection2(m_rCondaVariableNameModel,
            "Conda environment flow variable:", m_condaVariablesSupplier);
        m_rCondaVariableNameModel.addChangeListener(m_rHomeChangeListener);
        add(m_rHomeConda.getComponentPanel(), gbc);
        gbc.gridy++;

        m_pathSelection.addActionListener(l -> updateSelection());
        m_condaSelection.addActionListener(l -> updateSelection());

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
            if (m_pathSelection.isSelected()) {
                final Optional<FlowVariable> rHome = m_rHomePathModel.getVariableValue();
                if (rHome.isPresent()) {
                    return new DefaultRPreferenceProvider(rHome.get().getStringValue());
                } else {
                    return new DefaultRPreferenceProvider(m_rHomePath.getSelectedFile());
                }
            } else {
                final var condaVar = m_condaVariablesSupplier.get().get(m_rCondaVariableNameModel.getStringValue());
                if (condaVar != null) {
                    return new DefaultRPreferenceProvider(condaVar);
                } else {
                    return new DefaultRPreferenceProvider("");
                }
            }
        } else {
            return RPreferenceInitializer.getRProvider();
        }
    }

    private void updateSelection() {
        if (m_pathSelection.isSelected()) {
            m_rHomePath.setEnabled(true);
            m_rCondaVariableNameModel.setEnabled(false);
        } else {
            m_rHomePath.setEnabled(false);
            m_rCondaVariableNameModel.setEnabled(true);
        }
    }

    /** Load the configured R home from the snippet settings */
    void loadSettingsFrom(final RSnippetSettings settings) {
        m_rHomePath.removeChangeListener(m_rHomeChangeListener);
        m_rCondaVariableNameModel.removeChangeListener(m_rHomeChangeListener);
        m_overwriteRHome.setSelected(settings.isOverwriteRHome());
        m_rHomePath.setSelectedFile(settings.getRHomePath());
        overwriteRHomeChanged();
        try {
            final var settingStranslation = new NodeSettings("");
            settings.saveSettings(settingStranslation);
            m_rCondaVariableNameModel.loadSettingsFrom(settingStranslation);
        } catch (InvalidSettingsException e) {
            NodeLogger.getLogger(getClass()).error(e);
        }
        m_rCondaVariableNameModel.addChangeListener(m_rHomeChangeListener);
        m_rHomePath.addChangeListener(m_rHomeChangeListener);
        if (settings.hasRHomePath()) {
            m_pathSelection.setSelected(true);
        } else {
            m_condaSelection.setSelected(true);
        }
        updateSelection();
    }

    /** Save the configured R home to the snippet settings */
    void saveSettingsTo(final RSnippetSettings settings) {
        settings.setOverwriteRHome(m_overwriteRHome.isSelected());
        settings.setUseRHomePath(m_pathSelection.isSelected());
        settings.setRHomePath(m_rHomePath.getSelectedFile());
        settings.setRCondaVariableName(m_rCondaVariableNameModel.getStringValue());
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
        m_pathSelection.setEnabled(enabled);
        m_condaSelection.setEnabled(enabled);
        if (!enabled) {
            m_rHomePath.setEnabled(false);
            m_rCondaVariableNameModel.setEnabled(false);
        } else {
            updateSelection();
        }
        m_rHomeError.setEnabled(enabled);
        rHomeChanged();
    }

    /**
     * Called if the R home has changed to check the new R home and display errors. Note that this does not trigger an
     * update of the snippet.
     */
    private void rHomeChanged() {
        if (m_rEnvChecker != null && !m_rEnvChecker.isDone()) {
            m_rEnvChecker.cancel(true);
        }
        m_rEnvChecker = new REnvChecker(getRPreferenceProvider());
        m_rEnvChecker.execute();
    }

    private final class REnvChecker extends SwingWorkerWithContext<Pair<Boolean, Optional<String>>, String> {

        private RPreferenceProvider m_rPrefs;

        private REnvChecker(final RPreferenceProvider rPrefs) {
            m_rPrefs = rPrefs;
        }

        @Override
        protected Pair<Boolean, Optional<String>> doInBackgroundWithContext() throws Exception {
            if (m_rPrefs.getRHome() == null || m_rPrefs.getRHome().trim().isEmpty()) {
                if (m_pathSelection.isSelected()) {
                    return Pair.of(true, Optional.of("Please select the path to R home."));
                } else {
                    return Pair.of(true, Optional.of("Please select the conda environment."));
                }
            }
            publish("Checking R home...");
            Thread.sleep(200);
            try {
                final Optional<String> warning = RBinUtil.checkREnvionment(m_rPrefs, "Path to R home", false);
                return Pair.of(false, warning);
            } catch (final InvalidRHomeException e) {
                return Pair.of(true, Optional.of(e.getMessage()));
            }
        }

        @Override
        protected void doneWithContext() {
            // Get the result
            boolean error;
            Optional<String> message;
            try {
                final Pair<Boolean, Optional<String>> result = get();
                error = result.getLeft();
                message = result.getRight();
            } catch (final ExecutionException e) {
                // Could not check
                error = true;
                message = Optional.of("Could not check the R Environment.");
            } catch (final InterruptedException | CancellationException e) {
                // Nothing to do the check is not valid anymore
                return;
            }

            // Set or clean the error
            if (message.isPresent() && error) {
                setError(message.get());
            } else if (message.isPresent()) {
                setWarning(message.get());
            } else {
                clearError();
            }
        }

        @Override
        protected void processWithContext(final List<String> chunks) {
            final String message = chunks.get(chunks.size() - 1);
            setProcess(message);
        }

        /** Set the given R home error */
        private void setError(final String message) {
            m_hasError = true;
            setRHomeError(message, ERROR_ICON);
        }

        /** Set the given R home warning */
        private void setWarning(final String message) {
            m_hasError = false;
            setRHomeError(message, WARNING_ICON);
        }

        /** Set the given process message */
        private void setProcess(final String message) {
            setRHomeError(message, PROCESS_ICON);
        }

        /** Deletes the R home error. Called if the environment was checked without warnings or errors. */
        private void clearError() {
            m_hasError = false;
            setRHomeError("", null);
        }

        /** Set the R home error */
        private void setRHomeError(final String message, final Icon icon) {
            m_rHomeError.setIcon(icon);
            m_rHomeError.setText(String.format("<html><div style=\\\"width:%dpx;\\\">%s</div></html>",
                m_rHomeErrorWidth, message.replace("<", "&lt;")));
        }

    }
}
