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
 */
package org.knime.r;

import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;

public class RSnippetNodeConfig {

    /**
     * Get the input port definition
     *
     * @return the input port definition
     */
    public Collection<PortType> getInPortTypes() {
        final Collection<PortType> portTypes = new ArrayList<PortType>(4);
        portTypes.add(BufferedDataTable.TYPE);
        return portTypes;
    }

    /**
     * Get the output port definition
     *
     * @return the output port definition
     */
    protected Collection<PortType> getOutPortTypes() {
        final Collection<PortType> portTypes = new ArrayList<PortType>(4);
        portTypes.add(BufferedDataTable.TYPE);
        return portTypes;
    }

    /**
     * Text preceding to the r-script.
     *
     * @return the r-script prefix
     */
    protected String getScriptPrefix() {
        return "";
    }

    /**
     * Text appended to the r-script.
     *
     * @return the r-script suffix
     */
    protected String getScriptSuffix() {
        return "";
    }

    /**
     * The default script for this node.
     *
     * @return the default script
     */
    protected String getDefaultScript() {
        boolean inHasTable = false;
        for (final PortType portType : getInPortTypes()) {
            if (portType.equals(BufferedDataTable.TYPE)) {
                inHasTable = true;
                break;
            }
        }
        boolean outHasTable = false;
        boolean outHasView = false;
        for (final PortType portType : getOutPortTypes()) {
            if (portType.equals(BufferedDataTable.TYPE)) {
                outHasTable = true;
                break;
            } else if (portType.equals(ImagePortObject.TYPE)) {
                outHasView = true;
            }
        }
        // the source nodes
        if (getInPortTypes().size() <= 0) {
            if (outHasTable) {
                return "knime.out <- data.frame()";
            } else {
                return "R <- data.frame()";
            }
        } else {
            if (inHasTable && outHasView) {
                return "plot(knime.in)";
            } else if (outHasView) {
                return "plot(iris)";
            } else if (inHasTable && outHasTable) {
                return "knime.out <- knime.in";
            } else if (!inHasTable && outHasTable) {
                return "knime.out <- R";
            } else if (inHasTable && !outHasTable) {
                return "R <- knime.in";
            } else {
                return "R <- data.frame()";
            }
        }
    }
}
