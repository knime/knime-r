/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   19.09.2007 (thiel): created
 */
package org.knime.ext.r.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.ext.r.RCorePlugin;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RPreferencePage extends FieldEditorPreferencePage 
implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public RPreferencePage() {
        super(GRID);

        setPreferenceStore(RCorePlugin.getDefault().getPreferenceStore());
        setDescription("KNIME R preferences");
    }    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        FileFieldEditor rPath =
                new FileFieldEditor(
                        RPreferenceInitializer.PREF_R_PATH,
                        "Path to R executable", parent);
        addField(rPath);
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
