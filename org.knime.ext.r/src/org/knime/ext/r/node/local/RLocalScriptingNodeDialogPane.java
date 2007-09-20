/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   17.09.2007 (thiel): created
 */
package org.knime.ext.r.node.local;

import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * A dialog containing a multi line text field to specify the R code to run.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalScriptingNodeDialogPane extends RLocalNodeDialogPane {

    /**
     * @return a <code>SettingsModelString</code> instance containing
     * the R command to run.
     */
    static final SettingsModelString createCommandSettingsModel() {
        return new SettingsModelString("R_command", null);
    }
    
    /**
     * Constructor which creates a new instance of 
     * <code>RLocalScriptingNodeDialogPane</code>. A dialog component is added
     * which allows users to enter R code. 
     */
    public RLocalScriptingNodeDialogPane() {
        super();
        
        createNewGroup("R command");
        
        addDialogComponent(new DialogComponentMultiLineString(
                createCommandSettingsModel(), "", true, 5, 4));
        
        closeCurrentGroup();
    }
}
