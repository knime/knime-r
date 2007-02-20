/* 
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2007
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
import org.knime.core.util.KnimeEncryption;
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
     * Constructor. Specify the number of inputs and outputs required.
     * @param dataIns number of inputs.
     * @param dataOuts number of outputs.
     */
    RNodeModel(final int dataIns, final int dataOuts) {
        super(dataIns, dataOuts);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #validateSettings(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        readSettings(settings, false);        
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        readSettings(settings, true);
    }
    
    /**
     * Validates the settings in the specified settings object. If the write
     * flag is set true, it takes them over (sets them in the internal
     * variables).
     * 
     * @param settings the settings object to verify/read.
     * @param write if true, settings are taken over, otherwise they are just
     *            validated.
     */
    void readSettings(final NodeSettingsRO settings, final boolean write) {
        String host = settings.getString(RConstants.KEY_HOST, 
                RConstants.DEFAULT_HOST);
        int port = settings.getInt(RConstants.KEY_PORT, 
                RConstants.DEFAULT_PORT);
        String user = settings.getString(RConstants.KEY_USER, 
                RConstants.DEFAULT_USER);
        String pw = settings.getString(RConstants.KEY_PASSWORD, 
                    RConstants.DEFAULT_PASS);
        if (write) {
            RConstants.setHost(host);
            RConstants.setPort(port);
            RConstants.setUser(user);
            RConstants.setPassword(pw);
        }
        
    }
    
    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(RConstants.KEY_HOST, RConstants.getHost());
        settings.addInt(RConstants.KEY_PORT, RConstants.getPort());
        settings.addString(RConstants.KEY_USER, RConstants.getUser());
        settings.addString(RConstants.KEY_PASSWORD, RConstants.getPassword());
    }
    
    private static Rconnection mRCONN;
    
    private static final String RCONNECTION_ERROR
        = new String("Can't connect to Rserve. Make sure R server is "
                + "available before running this node.");
    
    
    /**
     * @return a new R connection
     */
    protected final Rconnection createRconnection() {
        mRCONN = null;
        return getRconnection();
    }
    
    /**
     * @return the current R connection; if not available, a new (static) one 
     *         will be created
     */
    protected final Rconnection getRconnection() {
        if (mRCONN != null && mRCONN.isConnected()) {
            return mRCONN;
        }
        LOGGER.info("Starting R evaluation on Rserve (user=" 
                + RConstants.getUser() + ",host="
                + RConstants.getHost() + ",port="
                + RConstants.getPort() + ")...");
        try {
            mRCONN = new Rconnection(RConstants.getHost() + " " 
                    + RConstants.getPort());
            if (mRCONN.needLogin()) {
                String pw = "";
                try {
                    KnimeEncryption.decrypt(RConstants.getPassword());
                } catch (Exception e) {
                    LOGGER.warn("Could not encrypt password.");
                } finally {
                    mRCONN.login(RConstants.getUser(), pw);
                }
            }
        } catch (RSrvException rse) {
            setWarningMessage(RCONNECTION_ERROR);
            mRCONN = null;
            IllegalStateException e = 
                new IllegalStateException(RCONNECTION_ERROR);
            e.initCause(rse);
            throw e;
        }
        if ((mRCONN == null) || (!mRCONN.isConnected())) {
            setWarningMessage(RCONNECTION_ERROR);
            throw new IllegalStateException(RCONNECTION_ERROR);
        }
        LOGGER.debug("R connection opened");
        return mRCONN;
    }
    
}
