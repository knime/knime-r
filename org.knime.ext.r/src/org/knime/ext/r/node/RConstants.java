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

/**
 * Defines variables to create a connection to a R server. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RConstants {
    
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
    
    private static String userName = DEFAULT_USER;
    
    private static String hostAdr = DEFAULT_HOST;
    
    private static int portNo = DEFAULT_PORT;
    
    private static String pass = DEFAULT_PASS;

    private RConstants() {
    }
    
    /**
     * Sets the username to the specified string.
     * @param user the new user name.
     */
    public static void setUser(final String user) {
        userName = user;
    }
    
    /**
     * @return the currently set username.
     */
    public static String getUser() {
        return userName;
    }
    
    /**
     * Sets the host ip address to the specified string.
     * @param host the new host IP address.
     */
    public static void setHost(final String host) {
        hostAdr = host;
    }
    
    /**
     * @return the currently set host IP address as string.
     */
    public static String getHost() {
        return hostAdr;
    }
    
    /**
     * Sets the port address to the specified number.
     * @param port the new port number.
     */
    public static void setPort(final int port) {
        portNo = port;
    }
    
    /**
     * @return the currently set port number.
     */
    public static int getPort() {
        return portNo;
    }
    
    /**
     * Sets the passphrase to the specified string.
     * @param password the new password.
     */
    public static void setPassword(final String password) {
        pass = password;
    }
    
    /**
     * @return the currently set password.
     */
    public static String getPassword() {
        return pass;
    }
}
