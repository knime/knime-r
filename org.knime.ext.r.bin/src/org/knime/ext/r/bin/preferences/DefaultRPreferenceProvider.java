/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   13.06.2014 (thor): created
 */
package org.knime.ext.r.bin.preferences;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.r.bin.RBinUtil;

import com.sun.jna.Platform;

/**
 * Default provider for R preferences. I determines the R binary path based on the R home given in the constructor.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @author Jonathan Hale
 */
public class DefaultRPreferenceProvider implements RPreferenceProvider {
    private static final String CONDA_EVN_TYPE_CLASS_NAME =
        "org.knime.python2.CondaEnvironmentPropagation.CondaEnvironmentType";

    private final String m_rHome;

    private final String m_condaPrefix;

    private Properties m_properties = null;

    /**
     * Creates a new preference provider based on the given R home directory.
     *
     * @param rHome R's home directory
     */
    public DefaultRPreferenceProvider(final String rHome) {
        m_condaPrefix = null;
        m_rHome = rHome;
    }

    /**
     * Creates a new preference provider using the given conda environment to derive the R home directory and binaries
     * from.
     *
     * @param condaVar the conda environment variable to use
     */
    public DefaultRPreferenceProvider(final FlowVariable condaVar) {
        CheckUtils.checkArgument(
            condaVar.getVariableType().getClass().getCanonicalName().equals(CONDA_EVN_TYPE_CLASS_NAME),
            "Variable is not a conda evironment variable.");
        final var rawString = condaVar.getValueAsString();
        // use the format of the string representation of the conda environment variable
        // `{name: <name>, prefix: <prefix>}`
        // and that a conda environment name may not contain a colon
        m_condaPrefix =
            rawString.substring(rawString.indexOf(':', rawString.indexOf(':') + 1) + 2, rawString.length() - 1);
        m_rHome = m_condaPrefix + File.separator + "lib" + File.separator + "R";
    }

    @Override
    public String getRHome() {
        return m_rHome;
    }

    @Override
    public int getMaxInfBuf() {
        return org.eclipse.core.runtime.Platform.getPreferencesService().getInt("org.knime.ext.r.bin",
            RPreferenceInitializer.PREF_RSERVE_MAXINBUF, 256, null);
    }

    @Override
    public String getRBinPath(final String command) {
        if (m_condaPrefix != null && Platform.isWindows()) {
            return m_condaPrefix + File.separator + "Scripts" + File.separator + command + ".exe";
        }

        final String binPath = getRHome() + File.separator + "bin" + File.separator;
        if (Platform.isWindows()) {
            if (Platform.is64Bit()) {
                return binPath + "x64" + File.separator + command + ".exe";
            } else {
                return binPath + "i386" + File.separator + command + ".exe";
            }
        } else {
            return binPath + command;
        }
    }

    @Override
    public String getRServeBinPath() {
        if (m_properties == null) {
            m_properties = RBinUtil.retrieveRProperties(this);
        }

        final String rservePath = (String)m_properties.get("Rserve.path") + File.separator + "libs" + File.separator;
        if (Platform.isWindows()) {
            if (Platform.is64Bit()) {
                return rservePath + "x64" + File.separator + "Rserve.exe";
            } else {
                return rservePath + "i386" + File.separator + "Rserve.exe";
            }
        } else {
            return rservePath + "Rserve";
        }
    }

    @Override
    public Map<String, String> setUpEnvironment(final Map<String, String> environment) {
        if (Platform.isWindows() && m_condaPrefix != null) {
            final var pathVar = new StringBuilder();
            pathVar.append(m_condaPrefix).append(File.pathSeparator);
            pathVar.append(m_condaPrefix).append(File.separator).append("Library").append(File.separator).append("bin")
                .append(File.pathSeparator);
            pathVar.append(m_condaPrefix).append(File.separator).append("Library").append(File.separator)
                .append("mingw-w64").append(File.separator).append("bin").append(File.pathSeparator);
            pathVar.append(m_condaPrefix).append(File.separator).append("Scripts").append(File.pathSeparator);
            pathVar.append(environment.getOrDefault("PATH", ""));
            environment.put("PATH", pathVar.toString());
        }
        return environment;
    }

    /**
     * Get the properties for this provider. Use this method to avoid calling {@link RBinUtil#retrieveRProperties()},
     * which launches an external R process to retrieve R properties.
     *
     * @return The properties for this provider.
     */
    public Properties getProperties() {
        if (m_properties == null) {
            m_properties = RBinUtil.retrieveRProperties(this);
        }

        return (Properties)m_properties.clone();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DefaultRPreferenceProvider)) {
            return false;
        }
        final DefaultRPreferenceProvider o = (DefaultRPreferenceProvider)obj;
        return m_rHome.equals(o.m_rHome);
    }

    @Override
    public int hashCode() {
        return m_rHome.hashCode();
    }
}
