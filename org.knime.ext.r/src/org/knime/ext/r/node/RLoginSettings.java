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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodedialog.DialogComponentPasswordField;

/**
 * Defines variables to create a connection to a R server. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RLoginSettings {
    
    /** key used to store settings. */
    static final String KEY_HOST = "host";
    /** key used to store settings. */
    static final String KEY_PORT = "port";
    /** key used to store settings. */
    static final String KEY_USER = "user";
    /** key used to store settings. */
    static final String KEY_PASSWORD = "password";
    
    /** Default initalization for host: localhost. */
    public static final String DEFAULT_HOST = "127.0.0.1";
    /** Default initalization for port: . */
    public static final int DEFAULT_PORT = 6311;
    /** Default initalization for user: (none).*/
    public static final String DEFAULT_USER = "";
    /** Default initalization for password: (none).*/
    public static final String DEFAULT_PASS = "";
    
    private String m_user = DEFAULT_USER;
    
    private String m_host = DEFAULT_HOST;
    
    private int m_port = DEFAULT_PORT;
    
    private String m_pass = DEFAULT_PASS;

    /**
     * Create new empty R login settings object.
     */
    RLoginSettings() {
        
    }
    
    /**
     * Sets the user name to the specified string.
     * @param user the new user name.
     */
    public void setUser(final String user) {
        m_user = user;
    }
    
    /**
     * @return the currently set user name.
     */
    public String getUser() {
        return m_user;
    }
    
    /**
     * Sets the host ip address to the specified string.
     * @param host the new host IP address.
     */
    public void setHost(final String host) {
        m_host = host;
    }
    
    /**
     * @return the currently set host IP address as string.
     */
    public String getHost() {
        return m_host;
    }
    
    /**
     * Sets the port address to the specified number.
     * @param port the new port number.
     */
    public void setPort(final int port) {
        m_port = port;
    }
    
    /**
     * @return the currently set port number.
     */
    public int getPort() {
        return m_port;
    }
    
    /**
     * Sets the pass phrase to the specified string.
     * @param password the new password.
     */
    public void setPassword(final String password) {
        m_pass = password;
    }
    
    /**
     * @return the currently set password.
     */
    public String getPassword() {
        return m_pass;
    }
    
    /**
     * Validate settings.
     * @param settings to validate
     * @throws InvalidSettingsException if settings are not valid
     */
    protected void validateSettings(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
            readSettings(settings, false);        
    }

    /**
     * Load validated settings.
     * @param settings to load
     * @throws InvalidSettingsException if settings could not be loaded
     */
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        readSettings(settings, true);
    }
    
    private void readSettings(final NodeSettingsRO settings, 
            final boolean write) 
        throws InvalidSettingsException {
        String host = settings.getString(KEY_HOST, DEFAULT_HOST);
        int port = settings.getInt(KEY_PORT, DEFAULT_PORT);
        String user = settings.getString(KEY_USER, DEFAULT_USER);
        String pw = "";
        try { 
            pw = settings.getString(KEY_PASSWORD, DEFAULT_PASS);
            pw = DialogComponentPasswordField.decrypt(pw);
        } catch (Exception e) {
            throw new InvalidSettingsException(
                        "Could not decrypt password", e);
        } 
        if (write) {
            setHost(host);
            setPort(port);
            setUser(user);
            setPassword(pw);
        }
        
    }
    
    
    /**
     * Save settings.
     * @param settings saved into
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(KEY_HOST, getHost());
        settings.addInt(KEY_PORT, getPort());
        settings.addString(KEY_USER, getUser());
        try {
            settings.addString(KEY_PASSWORD, 
                    DialogComponentPasswordField.encrypt(
                            getPassword().toCharArray()));
        } catch (Exception e) {
            settings.addString(KEY_PASSWORD, DEFAULT_PASS);
        }
    }
}
