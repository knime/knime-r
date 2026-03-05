/*
 * ------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.r;

import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.scripting.AbstractDefaultScriptingNodeDialog;
import org.knime.core.webui.node.dialog.scripting.AbstractFallbackScriptingNodeFactory;
import org.knime.node.parameters.NodeParameters;

/**
 * Factory for the <code>RSnippetNodeFactory</code> node.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("restriction")
public abstract class AbstractWebUIRSnippetNodeFactory extends AbstractFallbackScriptingNodeFactory<RSnippetNodeModel> {
    private final RSnippetNodeConfig m_config;

    private final Class<? extends NodeParameters> m_nodeParametersClass;

    /**
     * Constructor with config
     *
     * @param rSnippetModelConfig Used to configure the RSnippet node
     */
    public AbstractWebUIRSnippetNodeFactory(final RSnippetNodeConfig rSnippetModelConfig,
        final Class<? extends NodeParameters> nodeParametersClass) {
        m_config = rSnippetModelConfig;
        m_nodeParametersClass = nodeParametersClass;
    }

    @Override
    public RSnippetNodeModel createNodeModel() {
        return new RSnippetNodeModel(m_config);
    }

    @Override
    public int getNrNodeViews() {
        return 2;
    }

    @Override
    public NodeView<RSnippetNodeModel> createNodeView(final int viewIndex, final RSnippetNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new ExtToolStdoutNodeView<RSnippetNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStderrNodeView<RSnippetNodeModel>(nodeModel);
        }
        return null;
    }

    @Override
    public AbstractDefaultScriptingNodeDialog createNodeDialog() {
        return new RSnippetScriptingNodeDialog(m_nodeParametersClass);
    }

    @Override
    public NodeDialogPane createLegacyNodeDialogPane() {
        return new RSnippetNodeDialog(this.getClass(), m_config);
    }
}
