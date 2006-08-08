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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodedialog.DialogComponentPasswordField;
import org.rosuda.JRclient.RSrvException;
import org.rosuda.JRclient.Rconnection;


/**
 * R model to save and load login information for the R server.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
abstract class RNodeModel extends NodeModel {
    
    /** R Logger. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(RNodeModel.class);
    
    /**
     * 
     * @param dataIns
     * @param dataOuts
     */
    RNodeModel(final int dataIns, final int dataOuts) {
        super(dataIns, dataOuts);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        readSettings(settings, false);        
    }
    
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        readSettings(settings, true);
    }
    
    void readSettings(final NodeSettingsRO settings, final boolean write) 
        throws InvalidSettingsException {
        String host = settings.getString(RConstants.KEY_HOST, RConstants.DEFAULT_HOST);
        int    port = settings.getInt(RConstants.KEY_PORT, RConstants.DEFAULT_PORT);
        String user = settings.getString(RConstants.KEY_USER, RConstants.DEFAULT_USER);
        String pw = "";
        try { 
            pw = DialogComponentPasswordField.decrypt(
                    settings.getString(RConstants.KEY_PASSWORD, RConstants.DEFAULT_PASS));
        } catch (Exception e) {
            throw new InvalidSettingsException(
                        "Could not decrypt password", e);
        } 
        if (write) {
            RConstants.setHost(host);
            RConstants.setPort(port);
            RConstants.setUser(user);
            RConstants.setPassword(pw);
        }
        
    }
    
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(RConstants.KEY_HOST, RConstants.getHost());
        settings.addInt(RConstants.KEY_PORT, RConstants.getPort());
        settings.addString(RConstants.KEY_USER, RConstants.getUser());
    }
    
    private static Rconnection m_rconn;
    
    /**
     * @return The connection object to Rserve.
     */
    static final Rconnection getRconnection() {
        if (m_rconn != null && m_rconn.isConnected()) {
            return m_rconn;
        }
        LOGGER.info("Starting R evaluation on RServe (" + RConstants.getHost() + ") ...");
        try {
            m_rconn = new Rconnection(RConstants.getHost() + " " + RConstants.getPort());
            if (m_rconn.needLogin()) {
                m_rconn.login(RConstants.getUser(), RConstants.getPassword());
            }
        } catch (RSrvException rse) {
            LOGGER.error("Can't connect to server");
            throw new IllegalStateException("Make sure R Server is "
                    + "available before executing this node");
        }
        if ((m_rconn == null) || (!m_rconn.isConnected())) {
            LOGGER.error("Can't connect to server");
            throw new IllegalStateException("Make sure R Server is "
                    + "available before executing this node");
        }
        LOGGER.debug("R connection opened");
        return m_rconn;
    }
    
    static final void checkRconnection() throws InvalidSettingsException {
        try {
            getRconnection();
        } catch (Exception e) {
            throw new InvalidSettingsException("Check R login settings", e);
        }   
    }
}
