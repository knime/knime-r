/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as 
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node.local;

import org.knime.core.node.NodeFactory;
import org.knime.ext.r.preferences.RPreferenceProvider;

/**
 * Generic node factory for all R nodes.
 * 
 * @author Thomas Gabriel, KNIME.com, Zurich
 *
 * @param <T> typed node model derived from <code>RAbstractLocalNodeModel</code>
 */
public abstract class RNodeFactory<T extends RAbstractLocalNodeModel> 
        extends NodeFactory<T> {
    
    private final RPreferenceProvider m_pref; 
    
    /**
     * Node factory used for all local R nodes.
     * @param pref a preference provider
     */
    public RNodeFactory(final RPreferenceProvider pref) {
        super();
        m_pref = pref;
    }
    
    /**
     * @return R preference provider
     */
    public RPreferenceProvider getRProvider() {
        return m_pref;
    }

}
