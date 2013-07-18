/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the R integration plugin for KNIME.
 *
 * The R integration plugin is free software; you can redistribute 
 * it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.org.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.r;

import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 * Factory for the <code>RSource</code> node.
 *
 * @author Heiko Hofer
 */
public class RReaderTableNodeFactory extends RSnippetNodeFactory {

    /**
     * Empty default constructor.
     */
    public RReaderTableNodeFactory() {
    	super(new RSnippetNodeConfig() {
    		@Override
    		protected Collection<PortType> getInPortTypes() {
    			return Collections.EMPTY_LIST;
    		}
    		
    		@Override
    		protected Collection<PortType> getOutPortTypes() {
    			return Collections.singleton(BufferedDataTable.TYPE);
    		}
    		@Override
    		String getDefaultScript() {
    		    return "knime.out <- iris # assign your data frame here\n";
    		}
    	});
    }
  
}
