/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *
 */
package org.knime.ext.r.node;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.KnimeEncryption;

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

    /** Default initialization for user: (none). */
    public static final String DEFAULT_USER = "";

    /** Default initialization for password: (empty string). */
    public static final String DEFAULT_PASS = "";

    /** Settings model for user name. */
    static final SettingsModelString USER = createUserModel();

    /** Settings model for host name. */
    static final SettingsModelString HOST = createHostModel();

    /** Settings model for port number. */
    static final SettingsModelIntegerBounded PORT = createPortModel();

    /** Settings model for enrypted password field. */
    static final SettingsModelString PASS = createPasswordModel();

    /** @return settings model for the password. */
    static SettingsModelString createPasswordModel() {
        return new SettingsModelString(KEY_PASSWORD, DEFAULT_PASS);
    }

    /** @return settings model for the port. */
    static SettingsModelIntegerBounded createPortModel() {
        return new SettingsModelIntegerBounded(KEY_PORT, DEFAULT_PORT, 0, 65536);
    }

    /** @return settings model for the host. */
    static SettingsModelString createHostModel() {
        return new SettingsModelString(KEY_HOST, DEFAULT_HOST);
    }

    /** @return Settings model for the user. */
    static SettingsModelString createUserModel() {
        return new SettingsModelString(KEY_USER, DEFAULT_USER);
    }

    /**
     * Create new empty R login settings object.
     */
    private RLoginSettings() {
        // empty
    }

    /**
     * Sets the user name to the specified string.
     *
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
     *
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
     *
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
     * @return the currently set password - DECRYPTED.
     */
    static String getPassword() {
        final String pw = PASS.getStringValue();
        if ((pw == null) || (pw.length() == 0)) {
            return DEFAULT_PASS;
        }
        try {
            return KnimeEncryption.decrypt(pw);
        } catch (final Exception e) {
            return DEFAULT_PASS;
        }
    }

    /**
     * Validate settings.
     * 
     * @param settings to validate
     * @throws InvalidSettingsException if settings could not be validated
     */
    static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        USER.validateSettings(settings);
        HOST.validateSettings(settings);
        PASS.validateSettings(settings);
        PORT.validateSettings(settings);
    }

    /**
     * Load validated settings.
     * 
     * @param settings to load
     * @throws InvalidSettingsException if settings could not loaded
     */
    static void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        USER.loadSettingsFrom(settings);
        HOST.loadSettingsFrom(settings);
        PASS.loadSettingsFrom(settings);
        PORT.loadSettingsFrom(settings);
    }

    /**
     * Save settings.
     *
     * @param settings saved into
     */
    static void saveSettingsTo(final NodeSettingsWO settings) {
        USER.saveSettingsTo(settings);
        HOST.saveSettingsTo(settings);
        PORT.saveSettingsTo(settings);
        PASS.saveSettingsTo(settings);
    }
}
