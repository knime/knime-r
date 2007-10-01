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

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Defines variables to create a connection to a R server. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RLoginSettings {
    
    /** key used to store settings. */
    private static final String KEY_HOST = "host";
    /** key used to store settings. */
    private static final String KEY_PORT = "port";
    /** key used to store settings. */
    private static final String KEY_USER = "user";
    /** key used to store settings. */
    static final String KEY_PASSWORD = "password";
    
    /** Default initialization for host: localhost. */
    public static final String DEFAULT_HOST = "127.0.0.1";
    /** Default initialization for port: . */
    public static final int DEFAULT_PORT = 6311; // [0..65536]
    /** Default initialization for user: (none).*/
    public static final String DEFAULT_USER = "";
    /** Default initialization for password: (none).*/
    public static final String DEFAULT_PASS = "";
    
    /** Settings model for user name. */
    static final SettingsModelString USER = new SettingsModelString(
            KEY_USER, DEFAULT_USER);
    
    /** Settings model for host name. */
    static final SettingsModelString HOST = new SettingsModelString(
            KEY_HOST, DEFAULT_HOST);
    
    /** Settings model for port number. */
    static final SettingsModelIntegerBounded PORT = 
        new SettingsModelIntegerBounded(KEY_PORT, DEFAULT_PORT, 0, 65536);
    
    /** Settings model for enrypted password field. */
    static final SettingsModelString PASS = new SettingsModelString(
            KEY_PASSWORD, DEFAULT_PASS);
    /**
     * Create new empty R login settings object.
     */
    private RLoginSettings() {
        
    }
    
    /**
     * Sets the user name to the specified string.
     * @param user the new user name.
     */
    static void setUser(final String user) {
        USER.setStringValue(user);
    }
    
    /**
     * @return the currently set user name.
     */
    static String getUser() {
        return USER.getStringValue();
    }
    
    /**
     * Sets the host ip address to the specified string.
     * @param host the new host IP address.
     */
    static void setHost(final String host) {
        HOST.setStringValue(host);
    }
    
    /**
     * @return the currently set host IP address as string.
     */
    static String getHost() {
        return HOST.getStringValue();
    }
    
    /**
     * Sets the port address to the specified number.
     * @param port the new port number.
     */
    static void setPort(final int port) {
        PORT.setIntValue(port);
    }
    
    /**
     * @return the currently set port number.
     */
    static int getPort() {
        return PORT.getIntValue();
    }
    
    /**
     * Sets the pass phrase to the specified string.
     * @param password the new password.
     */
    static void setPassword(final String password) {
        String pw = "";
        try {
            pw = DialogComponentPasswordField.encrypt(password.toCharArray());
        } catch (Exception e) {
            // ignored
        }
        PASS.setStringValue(pw);
    }
    
    /**
     * @return the currently set password.
     */
    static String getPassword() {
        String pw = PASS.getStringValue();
        try {
            return DialogComponentPasswordField.decrypt(pw);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Validate settings.
     * @param settings to validate
     */
    static void validateSettings(final NodeSettingsRO settings) {
            readSettings(settings, false);        
    }

    /**
     * Load validated settings.
     * @param settings to load
     */
    static void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        readSettings(settings, true);
    }
    
    private static void readSettings(final NodeSettingsRO settings, 
            final boolean write) {
        String host = settings.getString(KEY_HOST, DEFAULT_HOST);
        int port = settings.getInt(KEY_PORT, DEFAULT_PORT);
        String user = settings.getString(KEY_USER, DEFAULT_USER);
        String pw = settings.getString(KEY_PASSWORD, DEFAULT_PASS);
        if (write) {
            PASS.setStringValue(pw);
            HOST.setStringValue(host);
            PORT.setIntValue(port);
            USER.setStringValue(user);
        }
    }   
    
    /**
     * Save settings.
     * @param settings saved into
     */
    static void saveSettingsTo(final NodeSettingsWO settings) {
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
