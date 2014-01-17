/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   05.12.2011 (hofer): created
 */
package org.knime.r;

import java.util.HashSet;
import java.util.Set;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

/**
 * The document used in the rsnippet dialogs.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class RSnippetDocument extends RSyntaxDocument {
	/**
	 * Style meaning don't syntax highlight anything.
	 */
	public static final String SYNTAX_STYLE_NONE = "text/plain";


	/**
	 * Style for highlighting R.
	 */
	public static final String SYNTAX_STYLE_R = "text/r";

    /**
     * Create a new instance.
     */
    public RSnippetDocument() {
        super(new RSnippetTokenMarkerFactory(), SYNTAX_STYLE_NONE);
       
    }
    
    public static class RSnippetTokenMarkerFactory  extends TokenMakerFactory {
    	private Set<String> m_syntaxStyles;
    	
    	public RSnippetTokenMarkerFactory() {
    		m_syntaxStyles = new HashSet<String>();
    		m_syntaxStyles.add(SYNTAX_STYLE_NONE);
    		m_syntaxStyles.add(SYNTAX_STYLE_R);
		}

		@Override
		protected TokenMaker getTokenMakerImpl(final String key) {
			if (m_syntaxStyles.contains(key)) {
				if (key.equals(SYNTAX_STYLE_R)) {
					return new RTokenMaker();
				} else {
					// In case of null return a PlainTextTokenMaker will be used
					return null;
				} 
			}
			return null;
		}

		@Override
		public Set keySet() {
			return m_syntaxStyles;
		}

    	
    }

   }
