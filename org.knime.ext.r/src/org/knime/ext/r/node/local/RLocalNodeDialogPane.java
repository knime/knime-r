/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2011
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.09.2007 (gabriel): created
 */
package org.knime.ext.r.node.local;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The <code>RLocalNodeDialogPane</code> is a
 * dialog pane providing a file chooser to select the R executable, as well
 * as a checkbox to specify which R executable will be used to execute the
 * R script. If the checkbox is <u>not</u> checked, the R executable file
 * specified in the KNIME-R preferences is used, if the checkbox <u>is</u>
 * checked the specified file of the file chooser dialog is used.
 * This dialog can be extended to take use of this functionality but be aware
 * to call the super constructor when extending
 * <code>RLocalNodeDialogPane</code>.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Kilian Thiel, University of Konstanz
 */
public abstract class RLocalNodeDialogPane extends DefaultNodeSettingsPane {

    /** Tab name for the R binary path. */
    private static final String TAB_R_BINARY = "R Binary";

    /**
     * @return Returns a <code>SettingsModelBoolean</code> instance specifying
     * if the determined R executable file is used.
     */
    static final SettingsModelBoolean createUseSpecifiedFileModel() {
        return new SettingsModelBoolean("R_use_specified_file", false);
    }

    private final SettingsModelBoolean m_smb;

    private final SettingsModelString m_fileModel;

    /**
     * Constructor of <code>RLocalNodeDialogPane</code> which provides a
     * default dialog component to specify the R executable file and a checkbox
     * to specify which R executable is used.
     */
    public RLocalNodeDialogPane() {
        super();

        // create setting models and add listener to model of checkbox.
        m_fileModel = createRBinaryFile();
        m_smb = createUseSpecifiedFileModel();
        m_smb.addChangeListener(new CheckBoxChangeListener());

        // create file chooser component.
        DialogComponentFileChooser fileChooser = new DialogComponentFileChooser(
                m_fileModel, "R_binarys", JFileChooser.OPEN_DIALOG,
                false, new String[]{"", ".exe"});

        setHorizontalPlacement(true);
        createNewGroup("R binary path");

        // create check box component
        DialogComponentBoolean checkbox = new DialogComponentBoolean(
                m_smb, "Override default:");
        checkbox.setToolTipText("If checked, the specified file is used "
                + "as R Binary. If not checked, the file specified in "
                + "the KNIME's R preferences is used.");

        addDialogComponent(checkbox);
        addDialogComponent(fileChooser);

        closeCurrentGroup();
        setHorizontalPlacement(false);

        enableFileChooser();
        setDefaultTabTitle(TAB_R_BINARY);
    }

    /**
     * @return a <code>SettingsModelString</code> instance containing the path
     *         to the R executable
     */
    static final SettingsModelString createRBinaryFile() {
        SettingsModelString sms = new SettingsModelString("R_binary_file", "");
        sms.setEnabled(false);
        return sms;
    }

    /**
     * Enable or disable file chooser model.
     *
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        enableFileChooser();
    }

    private class CheckBoxChangeListener implements ChangeListener {
        /** {@inheritDoc} */
        @Override
        public void stateChanged(final ChangeEvent e) {
            enableFileChooser();
        }
    }

    /**
     * Enables the file chooser model if checkbox is checked and disables it
     * when the checkbox is not checked.
     */
    private void enableFileChooser() {
        if (m_smb.getBooleanValue()) {
            m_fileModel.setEnabled(true);
        } else {
            m_fileModel.setEnabled(false);
        }
    }

}
