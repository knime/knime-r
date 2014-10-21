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
 */
package org.knime.ext.r.node.local.port;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
            try (BufferedReader reader = new BufferedReader(new FileReader(m_fileR))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }
            } catch (Exception e) {
                LOGGER.warn("R model could not be read from file!", e);
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
        if (m_fileR.equals(rPort.m_fileR)) {
            return true;
        }

        try (InputStream in1 = new FileInputStream(m_fileR); InputStream in2 = new FileInputStream(rPort.m_fileR)) {
            return IOUtils.contentEquals(in1, in2);
        } catch (IOException ex) {
            LOGGER.warn("R models in '" + m_fileR + "' and '" + rPort.m_fileR + "' could not be compared: "
                                + ex.getMessage(), ex);
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileR.hashCode();
    }
}
