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
 * History
 *   01.12.2011 (hofer): created
 */
package org.knime.r;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.knime.core.node.NodeLogger;

/**
 * The R snippet which can be controlled by changing the settings or by changing the contents of the snippets document.
 *
 * @author Heiko Hofer
 */
public final class RSnippet {
    /** Identifier for row index (starting with 0). */
    public static final String ROWINDEX = "ROWINDEX";

    /** Identifier for row ID. */
    public static final String ROWID = "ROWID";

    /** Identifier for row count. */
    public static final String ROWCOUNT = "ROWCOUNT";

    /** The version 1.x of the java snippet. */
    public static final String VERSION_1_X = "version 1.x";

    private RSyntaxDocument m_document;

    private RSnippetSettings m_settings;

    private NodeLogger m_logger;

    public RSnippet() {
        m_settings = new RSnippetSettings() {

	// public ValueReport<BufferedDataTable> execute(
	// final BufferedDataTable table,
	// final FlowVariableRepository flowVariableRepository,
	// final ExecutionContext exec) throws CanceledExecutionException,
	// InvalidSettingsException {
	// List<String> errors = new ArrayList<String>();
	// List<String> warnings = new ArrayList<String>();
	//
	// try {
	// RController r = RController.getDefault();
	// // TODO: lock controller
	// r.clearWorkspace();
	// if (table != null) {
	// r.exportDataTable(table, "knime.in", exec);
	// }
	//
	// r.timedEval(getDocument().getText(0, getDocument().getLength()));
	//
	// BufferedDataTable out = r.importBufferedDataTable("knime.out", exec);
	// // TODO: unlock controller
	// return new ValueReport<BufferedDataTable>(out, errors, warnings);
	//
	// } catch (Exception e) {
	// errors.add(e.getMessage());
	// m_logger.error(e);
	// return new ValueReport<BufferedDataTable>(null, errors, warnings);
	// }
	// }

	// /**
	// * The execution method when no input table is present. I.e. used by
	// * the java edit variable node.
	// * @param flowVariableRepository flow variables at the input
	// * @param exec the execution context to report progress, may be null when
	// * this method is called from configure
	// */
	// public void execute(final FlowVariableRepository flowVariableRepository,
	// final ExecutionContext exec) {
	// DataTableSpec spec = new DataTableSpec();
	// CellFactory factory = new JavaSnippetCellFactory(this, spec,
	// flowVariableRepository, 1);
	// factory.getCells(new DefaultRow(RowKey.createRowKey(0),
	// new DataCell[0]));
	// }

	// /** The rearranger is the working horse for creating the ouput table. */
	// private ColumnRearranger createRearranger(final DataTableSpec spec,
	// final FlowVariableRepository flowVariableRepository,
	// final int rowCount)
	// throws InvalidSettingsException {
	// int offset = spec.getNumColumns();
	// CellFactory factory = new JavaSnippetCellFactory(this, spec,
	// flowVariableRepository, rowCount);
	// ColumnRearranger c = new ColumnRearranger(spec);
	// // add factory to the column rearranger
	// c.append(factory);
	//
	// // define which new columns do replace others
	// OutColList outFields = m_fields.getOutColFields();
	// for (int i = outFields.size() - 1; i >= 0; i--) {
	// OutCol field = outFields.get(i);
	// int index = spec.findColumnIndex(field.getKnimeName());
	// if (index >= 0) {
	// if (field.getReplaceExisting()) {
	// c.remove(index);
	// c.move(offset + i - 1, index);
	// } else {
	// throw new InvalidSettingsException("Field \""
	// + field.getJavaName() + "\" is configured to "
	// + "replace no existing columns.");
	// }
	// }
	// }
	//
	// return c;
	// }
            @Override
            public String getScript() {
                if (m_document != null) {
                    try {
                        return m_document.getText(0, m_document.getLength());
                    } catch (final BadLocationException e) {
                        // never happens
                        throw new RuntimeException(e);
                    }
                } else {
                    return super.getScript();
                }
            }

            @Override
            public void setScript(final String script) {
                if (m_document != null) {
                    try {
                        final String s = m_document.getText(0, m_document.getLength());
                        if (!s.equals(script)) {
                            m_document.replace(0, m_document.getLength(), script, null);
                        }
                    } catch (final BadLocationException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
                super.setScript(script);
            }
        };

    }

    /**
     * Get the updated settings java snippet.
     *
     * @return the settings
     */
    public RSnippetSettings getSettings() {
        return m_settings;
    }

    /**
     * Get the document with the code of the snippet.
     *
     * @return the document
     */
    public RSyntaxDocument getDocument() {
        // Lazy initialization of the document
        if (m_document == null) {
            final String initScript = m_settings.getScript();
            m_document = createDocument();
            // this changes the document to, if present
            m_settings.setScript(initScript);
        }
        return m_document;
    }

    /** Create the document with the default skeleton. */
    private RSyntaxDocument createDocument() {
        final RSyntaxDocument doc = new RSnippetDocument();
        return doc;
    }

    /**
     * Execute the snippet.
     *
     * @param table the data table at the inport
     * @param flowVariableRepository the flow variables at the inport
     * @param exec the execution context to report progress
     * @return the table for the output
     * @throws InvalidSettingsException when settings are inconsistent with the table or the flow variables at the input
     * @throws CanceledExecutionException when execution is canceled by the user
     */

    /**
     * Create a template for this snippet.
     *
     * @param metaCategory the meta category of the template
     * @return the template with a new uuid.
     */
    @SuppressWarnings("rawtypes")
    public RSnippetTemplate createTemplate(final Class metaCategory) {
        final RSnippetTemplate template = new RSnippetTemplate(metaCategory, getSettings());
        return template;
    }

    /**
     * Attach logger to be used by this java snippet instance.
     *
     * @param logger the node logger
     */
    public void attachLogger(final NodeLogger logger) {
        m_logger = logger;
    }

}
