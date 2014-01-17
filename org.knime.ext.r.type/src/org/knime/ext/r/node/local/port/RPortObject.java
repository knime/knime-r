/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.io.IOUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;

/**
 * A port object for R model port providing a file containing a R model.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RPortObject implements PortObject {

    /** Convenience access member for
     * <code>new PortType(RPortObject.class)</code>. */
    public static final PortType TYPE = new PortType(RPortObject.class);

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RPortObject.class);

    private final File m_fileR;
    private final List<String> m_libraries;

    /**
     * Creates an instance of <code>RPortObject</code> with given file.
     *
     * @param fileR The file containing a R model.
     */
    public RPortObject(final File fileR) {
        this(fileR, Collections.<String>emptyList());
    }
    
    /** 
     * @param fileR The workspace file.
     * @param libraries The list of libraries, not null.
     * @since 2.8
     */
    public RPortObject(final File fileR, final List<String> libraries) {
        m_fileR = fileR;
        m_libraries = libraries.isEmpty() ? Collections.<String>emptyList() 
            : Collections.unmodifiableList(new ArrayList<String>(libraries));
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
    public File getFile() {
        return m_fileR;
    }

    /** Unmodifiable list of libraries needed in the workspace. (R workspaces don't persist their loaded libraries.)
     * @return List of loaded libraries, not null.
     * @since 2.8
     */
    public List<String> getLibraries() {
        return m_libraries;
    }

    /**
     * Serializer used to save this port object.
     * @return a {@link RPortObject}
     */
    public static PortObjectSerializer<RPortObject> getPortObjectSerializer() {
        return new PortObjectSerializer<RPortObject>() {
            /** {@inheritDoc} */
            @Override
            public void savePortObject(final RPortObject portObject, final PortObjectZipOutputStream out,
                final ExecutionMonitor exec)throws IOException, CanceledExecutionException {
                out.putNextEntry(new ZipEntry("knime.R"));
                FileInputStream fis = new FileInputStream(portObject.m_fileR);
                FileUtil.copy(fis, out);
                fis.close();
                out.putNextEntry(new ZipEntry("library.list"));
                IOUtils.writeLines(portObject.m_libraries, "\n", out, "UTF-8");
                out.closeEntry();
                out.close();
            }

            /** {@inheritDoc} */
            @SuppressWarnings("unchecked")
            @Override
            public RPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
                final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
                ZipEntry nextEntry = in.getNextEntry();
                if (nextEntry == null || !"knime.R".equals(nextEntry.getName())) {
                    throw new IOException("Expected zip entry \"knime.R\" but got " + nextEntry == null
                        ? "<null>"
                        : nextEntry.getName());
                }
                File fileR = FileUtil.createTempFile("~knime", ".R");
                FileOutputStream fos = new FileOutputStream(fileR);
                FileUtil.copy(in, fos);
                nextEntry = in.getNextEntry();
                List<String> libraries;
                if (nextEntry == null) {
                    // old style port object (2.7-)
                    libraries = Collections.emptyList();
                } else {
                    libraries = IOUtils.readLines(in, "UTF-8");
                }
                in.close();
                fos.close();
                return new RPortObject(fileR, libraries);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JPanel panel = new JPanel(new BorderLayout());
        JEditorPane jep = new JEditorPane();
        jep.setEditable(false);
        panel.setName("R Port View");
        jep.setText("R model file:\n" + getFilePath());
        panel.add(new JScrollPane(jep));
        return new JComponent[]{panel};
    }

    /**
     * @return The path of the R model file if available, otherwise
     * "No file available".
     */
    String getFilePath() {
        if (m_fileR != null) {
            return m_fileR.getAbsolutePath();
        }
        return "No file available";
    }

    /**
     * @return The input of the R model file.
     */
    String getModelData() {
        StringBuffer buf = new StringBuffer();
        if (m_fileR != null && m_fileR.exists() && m_fileR.canRead()) {
            try {
                BufferedReader reader =
                        new BufferedReader(new FileReader(m_fileR));
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }
            } catch (Exception e) {
                LOGGER.warn("R model could not be read from file!");
                buf.append("R model could no be read from file!");
            }
        }
        return buf.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RPortObject)) {
            return false;
        }
        RPortObject rPort = (RPortObject) obj;
        return m_fileR.equals(rPort.m_fileR);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileR.hashCode();
    }
}
