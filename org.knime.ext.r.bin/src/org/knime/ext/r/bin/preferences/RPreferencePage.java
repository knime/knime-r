/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   19.09.2007 (thiel): created
 *   05.11.2015 (hale): Improved and adapted to new Rserve backend for R nodes
 */
package org.knime.ext.r.bin.preferences;

import java.util.Optional;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.ext.r.bin.Activator;
import org.knime.ext.r.bin.RBinUtil;
import org.knime.ext.r.bin.RBinUtil.InvalidRHomeException;

/**
 * Preference page for settings the R installation directory.
 *
 * @author Jonathan Hale
 * @author Heiko Hofer
 */
public class RPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /* Maximal value for the receive buffer size */
    private static final int MAX_RECEIVE_BUFFER_SIZE = 1000000;

    /**
     * Constructor.
     */
    public RPreferencePage() {
        super(GRID);
    }

    /**
     * Modified DirectoryFieldEditor with an appropirate doCheckState() override and verification on key stroke.
     *
     * @author Jonathan Hale
     */
    private class RHomeDirectoryFieldEditor extends DirectoryFieldEditor {
        /**
         * @param name
         * @param labelText
         * @param parent
         */
        public RHomeDirectoryFieldEditor(final String name, final String labelText, final Composite parent) {
            // we do most of the parent code, but set a different validate strategy.
            super.init(name, labelText);
            setChangeButtonText(JFaceResources.getString("openBrowse")); //$NON-NLS-1$
            setValidateStrategy(VALIDATE_ON_KEY_STROKE);
            createControl(parent);
        }

        @Override
        protected boolean doCheckState() {
            return checkRVersion(getStringValue());
        }
    }

    @Override
    protected void createFieldEditors() {
        addField(new RHomeDirectoryFieldEditor(RPreferenceInitializer.PREF_R_HOME, "Path to R Home",
            getFieldEditorParent()));
        final IntegerFieldEditor field = new IntegerFieldEditor(RPreferenceInitializer.PREF_RSERVE_MAXINBUF,
            "Rserve receiving buffer size limit\n(in MB -- 0 for unlimited)", getFieldEditorParent());
        field.setValidRange(0, MAX_RECEIVE_BUFFER_SIZE);
        addField(field);

        checkRVersion(Activator.getRHOME().getAbsolutePath());
    }

    private boolean checkRVersion(final String rHome) {
        try {
            final Optional<String> warning = RBinUtil.checkREnvionment(rHome, "Path to R Home", false);
            if (warning.isPresent()) {
                setMessage(warning.get(), WARNING);
            } else {
                setMessage(null, NONE);
            }
            return true;
        } catch (final InvalidRHomeException e) {
            setMessage(e.getMessage(), ERROR);
            return false;
        }
    }

    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("KNIME R preferences");
    }
}
