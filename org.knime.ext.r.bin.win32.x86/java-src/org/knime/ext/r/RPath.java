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
 *   25.09.2008 (thor): created
 */
package org.knime.ext.r;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.Bundle;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class RPath {
    private RPath() {}
    public static File getRInstallationDir(final Bundle bundle) {
        URL url = bundle.getEntry("/");
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
