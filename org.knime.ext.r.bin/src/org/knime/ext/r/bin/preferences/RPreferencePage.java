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
 *   19.09.2007 (thiel): created
 */
package org.knime.ext.r.bin.preferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger;
import org.knime.ext.r.bin.Activator;
import org.knime.ext.r.bin.RBinUtil;

/**
 * Preference page for settings the R installation directory.
 *
 * @author Heiko Hofer
 */
public class RPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    private class MyDirectoryFieldEditor extends DirectoryFieldEditor {
        MyDirectoryFieldEditor(final String name, final String labelText, final Composite parent) {
            super(name, labelText, parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void valueChanged() {
            super.valueChanged();
            checkRVersion(getStringValue());
        }
    }

    /**
     * Creates a new preference page.
     */
    public RPreferencePage() {
        super(GRID);

        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("KNIME R preferences");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        DirectoryFieldEditor rHomePath =
            new MyDirectoryFieldEditor(RPreferenceInitializer.PREF_R_HOME, "Path to R Home", parent);
        addField(rHomePath);

        checkRVersion(Activator.getRHOME().getAbsolutePath());
    }

    void checkRVersion(final String rHome) {
        Path rHomePath = Paths.get(rHome);
        if (!Files.isDirectory(rHomePath)) {
            return;
        }

        RPreferenceProvider prefProvider = new DefaultRPreferenceProvider(rHome);
        try {
            String rHomeCheck = RBinUtil.getDefault().checkRHome(rHome);
            if (rHomeCheck != null) {
                int index = rHomeCheck.indexOf('.');
                setMessage(rHomeCheck.substring(0, index + 1), IMessageProvider.ERROR);
            } else {
                Properties props = RBinUtil.getDefault().retrieveRProperties(prefProvider);
                String version = props.getProperty("major") + "." + props.getProperty("minor");
                version = version.replace(" ", ""); // the version numbers may contains spaces
                if ("3.1.0".equals(version)) {
                    setMessage("You have selected an R 3.1.0 installation. "
                        + "Please see http://tech.knime.org/faq#q26 for details.", IMessageProvider.WARNING);
                }
            }
        } catch (IOException | InterruptedException ex) {
            // too bad
            NodeLogger.getLogger(getClass()).info("Could not determine R version: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
