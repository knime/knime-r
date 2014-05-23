/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.r;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * The settings of the java snippet node.
 *
 * @author Heiko Hofer
 */
public class RSnippetSettings {
    private static final String SCRIPT = "script";
    private static final String TEMPLATE_UUID = "templateUUID";
    private static final String VERSION = "version";
    private static final String OUT_NON_NUMBERS_AS_MISSING = "Output non numbers (NaN, Inf, -Inf) as missing cells";

    /** Custom script. */
    private String m_script;
    
    /** The UUID of the blueprint for this setting. */
    private String m_templateUUID;    

    /** The version of the java snippet. */
    private String m_version;
    
    /** whether NaN, Inf and -Inf should be treated as missing values. Added in v2.10 for backward compatibility. */ 
	private boolean m_outNonNumbersAsMissing;


    /**
     * Create a new instance.
     */
    public RSnippetSettings() {
        m_script = "";
        m_templateUUID = null;
        m_version = RSnippet.VERSION_1_X;
        m_outNonNumbersAsMissing = false;
    }


    /** Saves current parameters to settings object.
     * @param settings To save to.
     */
    public void saveSettings(final ConfigWO settings) {
        settings.addString(SCRIPT, getScript());
        settings.addString(TEMPLATE_UUID, getTemplateUUID());
        settings.addString(VERSION, getVersion());
        settings.addBoolean(OUT_NON_NUMBERS_AS_MISSING, m_outNonNumbersAsMissing);
    }

    /** Loads parameters in NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final ConfigRO settings)
            throws InvalidSettingsException {
        setScript(settings.getString(SCRIPT));
        if (settings.containsKey(TEMPLATE_UUID)) {
            setTemplateUUID(settings.getString(TEMPLATE_UUID));
        }  
        if (settings.containsKey(OUT_NON_NUMBERS_AS_MISSING)) { // added in 2.10
        	setOutNonNumbersAsMissing(settings.getBoolean(OUT_NON_NUMBERS_AS_MISSING));
        } else {
        	// keep backward compatibility
        	setOutNonNumbersAsMissing(true);
        }
        setVersion(settings.getString(VERSION));
    }


    /** Loads parameters in Dialog.
     * @param settings To load from.
     */
    public void loadSettingsForDialog(final ConfigRO settings) {
    	setScript(settings.getString(SCRIPT, ""));
    	setTemplateUUID(settings.getString(TEMPLATE_UUID, null));        
    	setVersion(settings.getString(VERSION, RSnippet.VERSION_1_X));
        if (settings.containsKey(OUT_NON_NUMBERS_AS_MISSING)) {
        	setOutNonNumbersAsMissing(settings.getBoolean(OUT_NON_NUMBERS_AS_MISSING, false));
        } else {
        	// keep backward compatibility
        	setOutNonNumbersAsMissing(settings.getBoolean(OUT_NON_NUMBERS_AS_MISSING, true));
        }
    }

	public void loadSettings(final RSnippetSettings s) {
		setScript(s.getScript());
		setTemplateUUID(s.getTemplateUUID());
		setVersion(s.getVersion());
		setOutNonNumbersAsMissing(s.getOutNonNumbersAsMissing());
	}



    /**
     * @return the script
     */
    String getScript() {
        return m_script;
    }


    /**
     * @param script the script to set
     */
    void setScript(final String script) {
        m_script = script;
    }


    /**
     * @return the templateUUID
     */
    public String getTemplateUUID() {
        return m_templateUUID;
    }


    /**
     * @param templateUUID the templateUUID to set
     */
    void setTemplateUUID(final String templateUUID) {
        m_templateUUID = templateUUID;
    }
    
    /**
     * @return the version
     */
    String getVersion() {
        return m_version;
    }


    /**
     * @param version the version to set
     */
    void setVersion(final String version) {
        m_version = version;
    }


    /**
     * True when the R values NaN, Inf and -Inf should be treated as missing values.
     * Applies only to R nodes with table output.
     * @return when NaN, Inf and -Inf should be treated as missing values
     */
	boolean getOutNonNumbersAsMissing() {
		return m_outNonNumbersAsMissing;
	}
	
	
	/**
	 * Set if the R values NaN, Inf and -Inf should be treated as missing values.
	 * Applies only to R nodes with table output.
	 * @param outNonNumbersAsMissing whether NaN, Inf and -Inf should be treated as missing values
	 */
	void setOutNonNumbersAsMissing(boolean outNonNumbersAsMissing) {
		m_outNonNumbersAsMissing = outNonNumbersAsMissing;
	}


}
