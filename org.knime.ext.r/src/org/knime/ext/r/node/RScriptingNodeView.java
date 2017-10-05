/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.ext.r.node;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * <code>NodeView</code> and "RScripting" Node view.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@Deprecated
public class RScriptingNodeView extends NodeView<RScriptingNodeModel> {

    private final JEditorPane m_shell;

    private final JTextArea m_output;

    private final JList m_list;

    private final DefaultListModel m_listModel;

    private static final String FILE_NAME = "bild.png";

    /** If smaller the picture is not displayed. */
    private static final int MIN_FILE_SIZE = 500;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RScriptingNodeView.class);

    /** Underlying R model. */
    private final RRemoteNodeModel m_rModel;

    /**
     * Creates a new view.
     *
     * @param nodeModel The R Scripting model.
     */
    protected RScriptingNodeView(final RScriptingNodeModel nodeModel) {
        super(nodeModel);
        m_rModel = nodeModel;
        // create output view
        m_output = new JTextArea();
        m_output.setFont(new Font("Courier", Font.PLAIN, 12));
        m_output.setEditable(false);
        m_output.setText("");
        // create shell
        m_shell = new JEditorPane();
        m_shell.setText("");
        m_shell.setFont(new Font("Courier", Font.PLAIN, 12));
        // m_shell.setWrapStyleWord(false);
        m_shell.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    final String[] text = m_shell.getText().split("\n");
                    //if (text.length > 0) {
                    for (int i = text.length; --i >= 0;) {
                        String cmd = text[i];
                        cmd = cmd.replace('\r', ' ');
                        cmd = cmd.replace('\t', ' ');
                        cmd = cmd.trim();
                        if (cmd.length() == 0) {
                            continue; // try next line
                        }
                        LOGGER.debug("eval: " + text[text.length - 1]);
                        try {
                            createPNG(); // has to be there before command is
                            // send
                            final REXP rexp = m_rModel.getRconnection().eval("try(" + cmd + ")");
                            print(rexp, cmd);
                        } catch (final Exception exc) {
                            m_output.append(m_rModel.getRconnection().getLastError() + "\n");
                        } finally {
                            m_shell.requestFocus();
                        }
                        break;
                    }
                }
            }
        });
        // init column list
        m_listModel = new DefaultListModel();
        m_list = new JList(m_listModel);
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Object o = m_list.getSelectedValue();
                if (o != null) {
                    final DataColumnSpec cspec = (DataColumnSpec)o;
                    m_shell.replaceSelection(cspec.getName());
                    m_list.clearSelection();
                    m_shell.requestFocus();
                }
            }
        });
        final JPanel panel = new JPanel(new BorderLayout());
        final JScrollPane listScroll = new JScrollPane(m_list);
        listScroll.setPreferredSize(new Dimension(150, 250));
        panel.add(listScroll, BorderLayout.WEST);
        final JScrollPane shellScroll = new JScrollPane(m_shell);
        shellScroll.setPreferredSize(new Dimension(250, 250));
        panel.add(shellScroll, BorderLayout.CENTER);
        final JScrollPane outputScroll = new JScrollPane(m_output);
        outputScroll.setPreferredSize(new Dimension(400, 150));
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, outputScroll);
        super.setComponent(split);
    }

    private void print(final REXP rexp, final String cmd) {
        Image image = null;
        try {
            image = getImage();
        } catch (final Exception e) {
            LOGGER.error("Could not create image: ", e);
        }
        if (image != null) {
            Frame frame = null;
            Component comp = super.getComponent();
            while (comp != null) {
                if (comp instanceof Frame) {
                    frame = (Frame)comp;
                    break;
                }
                comp = comp.getParent();
            }
            final JDialog f = new JDialog(frame, super.getViewTitle() + " - " + cmd);
            final Container cont = f.getContentPane();
            cont.add(new RPlotterViewPanel(image));
            f.pack();
            f.setVisible(true);
        }
        m_output.append(rexp.toString() + "\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        m_shell.setText("");
        m_listModel.removeAllElements();
        final RScriptingNodeModel model = super.getNodeModel();
        final DataTableSpec inSpec = model.getDataTableSpec();
        if (inSpec != null) {
            final DataTableSpec spec = RConnectionRemote.createRenamedDataTableSpec(inSpec);
            for (int i = 0; i < spec.getNumColumns(); i++) {
                final DataColumnSpec cspec = spec.getColumnSpec(i);
                m_listModel.addElement(cspec);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // empty
    }

    private void createPNG() throws RserveException, REXPMismatchException {
        // we are careful here - not all R binaries support png
        // so we rather capture any failures
        final REXP xp = m_rModel.getRconnection().eval("try(png(\"" + FILE_NAME + "\"))");
        if (xp.asString() != null) { // if there's a string then we have a
            // problem, R sent an error
            LOGGER.warn("Can't open png graphics device:\n" + xp.asString());
            // this is analogous to 'warnings', but for us it's sufficient to
            // get just the 1st warning
            final REXP w = m_rModel.getRconnection().eval(
                "if (exists(\"last.warning\") && " + "length(last.warning)>0) names(last.warning)[1] " + "else 0");
            if (w.asString() != null) {
                LOGGER.warn(w.asString());
            }
        }
    }

    private final Image getImage() throws IOException, RserveException {
        m_rModel.getRconnection().voidEval("dev.off()");
        // the file should be ready now, so let's read (ok this isn't pretty,
        // but hey, this ain't no beauty contest *grin* =)
        // we read in chunks of bufSize (64k by default) and store the resulting
        // byte arrays in a vector
        // ... just in case the file gets really big ...
        // we don't know the size in advance, because it's just a stream.
        // also we can't rewind it, so we have to store it piece-by-piece
        final RFileInputStream is = m_rModel.getRconnection().openFile(FILE_NAME);
        final Vector<byte[]> buffers = new Vector<byte[]>();
        final int bufSize = 65536;
        byte[] buf = new byte[bufSize];
        int imgLength = 0;
        int n = 0;
        while (true) {
            n = is.read(buf);
            if (n == bufSize) {
                buffers.addElement(buf);
                buf = new byte[bufSize];
            }
            if (n > 0) {
                imgLength += n;
            }
            if (n < bufSize) {
                break;
            }
        }

        if (imgLength < MIN_FILE_SIZE) {
            return null;
        }
        LOGGER.info("The image " + FILE_NAME + " has " + imgLength + " bytes.");

        // now let's join all the chunks into one, big array ...
        final byte[] imgCode = new byte[imgLength];
        int imgPos = 0;
        for (final Enumeration<byte[]> e = buffers.elements(); e.hasMoreElements();) {
            final byte[] b = e.nextElement();
            System.arraycopy(b, 0, imgCode, imgPos, bufSize);
            imgPos += bufSize;
        }
        if (n > 0) {
            System.arraycopy(buf, 0, imgCode, imgPos, n);
        }

        // ... and close the file ... and remove it - we have what we need :)
        is.close();
        // TODO m_rConn.removeFile(fileName);

        // now this is pretty boring AWT stuff, nothing to do with R ...
        return Toolkit.getDefaultToolkit().createImage(imgCode);
    }
}
