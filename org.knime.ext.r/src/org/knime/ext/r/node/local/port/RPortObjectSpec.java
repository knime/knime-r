/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.09.2008 (gabriel): created
 */
package org.knime.ext.r.node.local.port;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

/**
 * A port object spec for R model port.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RPortObjectSpec implements PortObjectSpec {
    
    /**
     * The port object spec instance.
     */
    public static final RPortObjectSpec INSTANCE = new RPortObjectSpec();

    /**
     * Creating a new instance of <code>RPortObjectSpec</code>.
     */
    private RPortObjectSpec() { }
    
    /**
     * Serializer used to save this port object spec.
     * @return a {@link RPortObjectSpec}
     */
    public static PortObjectSpecSerializer<RPortObjectSpec> 
            getPortObjectSpecSerializer() {
        return new PortObjectSpecSerializer<RPortObjectSpec>() {
            /** {@inheritDoc} */
            @Override
            public RPortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in)
                    throws IOException {
                return INSTANCE;
            }
            /** {@inheritDoc} */
            @Override
            public void savePortObjectSpec(final RPortObjectSpec portObjectSpec,
                    final PortObjectSpecZipOutputStream out) 
                    throws IOException {
                
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[]{};
    }

}
