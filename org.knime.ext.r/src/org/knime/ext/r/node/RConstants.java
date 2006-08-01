/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
    
    static final String KEY_HOST = "host";
    static final String KEY_PORT = "port";
    static final String KEY_USER = "user";
    static final String KEY_PASSWORD = "password";
    
    /** Default initalization for host: localhost. */
    public static final String DEFAULT_HOST = "127.0.0.1";
    /** Default initalization for port: . */
    public static final int DEFAULT_PORT = 6311;
    /** Default initalization for user: (none).*/
    public static final String DEFAULT_USER = "";
    /** Default initalization for password: (none).*/
    public static final String DEFAULT_PASS = "";
    
    private static String USER = DEFAULT_USER;
    
    private static String HOST = DEFAULT_HOST;
    
    private static int PORT = DEFAULT_PORT;
    
    private static String PASS = DEFAULT_PASS;

    private RConstants() {
    }
    
    public static void setUser(final String user) {
        USER = user;
    }
    
    public static String getUser() {
        return USER;
    }
    
    public static void setHost(final String host) {
        HOST = host;
    }
    
    public static String getHost() {
        return HOST;
    }
    
    public static void setPort(final int port) {
        PORT = port;
    }
    
    public static int getPort() {
        return PORT;
    }
    
    public static void setPassword(final String password) {
        PASS = password;
    }
    
    public static String getPassword() {
        return PASS;
    }
}
