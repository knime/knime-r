/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.09.2008 (gabriel): created
 */
package org.knime.ext.r.node.local.port;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.util.FileUtil;

/**
 * A port object for R model port providing a file containing a R model. 
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class RPortObject implements PortObject {
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RPortObject.class);    
    
    private final File m_pmmlR;
    
    /**
     * Creates an instance of <code>RPortObject</code> with given file.
     * 
     * @param pmmlR The file containing a R model.
     */
    public RPortObject(final File pmmlR) {
        m_pmmlR = pmmlR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RPortObjectSpec getSpec() {
        return RPortObjectSpec.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return "R Object";
    }
    
    /**
     * Returns the file containing the R model.
     * 
     * @return the file containing the R model.
     */
    public File getPmmlFile() {
        return m_pmmlR;
    }
    
    /**
     * Serializer used to save this port object.
     * @return a {@link RPortObject}
     */
    public static PortObjectSerializer<RPortObject> 
            getPortObjectSerializer() {
        return new PortObjectSerializer<RPortObject>() {
            /** {@inheritDoc} */
            @Override
            public void savePortObject(final RPortObject portObject,
                    final PortObjectZipOutputStream out, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                out.putNextEntry(new ZipEntry("pmml.R"));
                FileInputStream fis = new FileInputStream(portObject.m_pmmlR);
                FileUtil.copy(fis, out);
                fis.close();
                out.close();
            }
            
            /** {@inheritDoc} */
            @Override
            public RPortObject loadPortObject(
                    final PortObjectZipInputStream in, 
                    final PortObjectSpec spec, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                in.getNextEntry();
                File pmmlR = File.createTempFile("~knime_pmml", ".R");
                FileOutputStream fos = new FileOutputStream(pmmlR);
                FileUtil.copy(in, fos);
                in.close();
                fos.close();
                return new RPortObject(pmmlR);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JPanel panel = new JPanel(new BorderLayout());
        if (m_pmmlR != null && m_pmmlR.exists() && m_pmmlR.canRead()) {
            JEditorPane jep = new JEditorPane();
            jep.setEditable(false);
            
            StringBuffer buf = new StringBuffer();
            
            try {
                BufferedReader reader = new BufferedReader(
                        new FileReader(m_pmmlR));
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }
            } catch (Exception e) {
                LOGGER.warn("R model could not be read from file!");
                buf.append("R model could no be read from file!");
            }
            
            jep.setText(buf.toString());
            panel.add(new JScrollPane(jep));
        }
        
        return new JComponent[]{panel};
    }

}
