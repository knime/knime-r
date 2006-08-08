/*
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2006
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.r.node;

import org.knime.core.node.Node;

import org.knime.base.node.io.filereader.FileReaderNodeFactory;

/**
 * Tests the RPlotter.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class RPlotterFlow {
    
    private RPlotterFlow(final String fileName) {
        Node file = new Node(new FileReaderNodeFactory(fileName));
        file.showDialog();
        file.execute();
        Node r = new Node(new RPlotterNodeFactory());
        r.getInPort(0).connectPort(file.getOutPort(0));
        r.showDialog();
        r.execute();
        r.showView(0);
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        new RPlotterFlow("../dataset/satimage/larrys_ocean_satimage.trn.xml");
        //new RPlotterFlow("../dataset/iris/data.all.xml");
    }

}
