/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
 *   May 3, 2009 (wiswedel): created
 */
package org.knime.ext.r.node.io;

import org.knime.base.node.io.portobject.PortObjectWriterNodeFactory;
import org.knime.core.node.port.PortType;
import org.knime.ext.r.node.local.port.RPortObject;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RPortObjectWriterNodeFactory extends
        PortObjectWriterNodeFactory {
    
    /**
     * 
     */
    public RPortObjectWriterNodeFactory() {
        // TODO replace arg by RPortObject.TYPE (one it is API standard)
        super(new PortType(RPortObject.class));
    }

}