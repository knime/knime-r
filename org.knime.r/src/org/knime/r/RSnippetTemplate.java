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
 * History
 *   02.04.2012 (hofer): created
 */
package org.knime.r;

import java.util.UUID;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A settings class for Java Snippet templates.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public class RSnippetTemplate {
    private static final String SNIPPET = "Snippet";

    private static final String META_CATEGORY = "Meta category";

    private static final String CATEGORY = "Category";

    private static final String NAME = "Name";

    private static final String DESCRIPTION = "Description";

    private static final String VERSION = "Version";

    /** The first version of templates. */
    public static final String VERSION_1_X = "version 1.x";

    private RSnippetSettings m_snippetSettings;

    /**
     * The meta category which typically is name of the node factory class this template comes from.
     */
    private String m_metaCategory;

    /** The category this template falls into. */
    private String m_category;

    /**
     * A short (one sentence) descriptive name. It must not necessarily be unique.
     */
    private String m_name;

    /** The description of the template. */
    private String m_description;

    /** The version of the template. */
    private String m_version;

    /** The uuid of the template. */
    private String m_uuid;

    /**
     * Create a template and read parameters from the settings object.
     *
     * @param settings the settings
     * @return a new instance
     */
    public static RSnippetTemplate create(final NodeSettingsRO settings) {
        final RSnippetTemplate template = new RSnippetTemplate();
        template.loadSettings(settings);
        return template;
    }

    /**
     * Create instance with default values.
     *
     * @param metaCategory the meta category of the template
     * @param snippetSettings the settings
     */
    public RSnippetTemplate(final Class<?> metaCategory, final RSnippetSettings snippetSettings) {
        this(metaCategory.getName(), snippetSettings);
    }

    /**
     * Create instance with default values.
     *
     * @param metaCategory the meta category of the template
     * @param snippetSettings the settings
     */
    public RSnippetTemplate(final String metaCategory, final RSnippetSettings snippetSettings) {
        m_metaCategory = metaCategory;
        m_category = "default";
        m_description = "";
        m_version = RSnippetTemplate.VERSION_1_X;
        m_snippetSettings = snippetSettings;
        m_uuid = UUID.randomUUID().toString();
        m_snippetSettings.setTemplateUUID(m_uuid);
    }

    /**
     * Create an empty instance used for persistence.
     */
    private RSnippetTemplate() {
        // fields will be set with loadSettingsFor...
    }

    /**
     * @return the snippetSettings
     */
    public RSnippetSettings getSnippetSettings() {
        return m_snippetSettings;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return m_category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(final String category) {
        m_category = category;
    }

    /**
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * @return the metaCategory
     */
    public String getMetaCategory() {
        return m_metaCategory;
    }

    /**
     * @return the uuid
     */
    public String getUUID() {
        return m_uuid;
    }

    /**
     * Saves current parameters to settings object.
     *
     * @param settings To save to.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(META_CATEGORY, m_metaCategory);
        settings.addString(CATEGORY, m_category);
        settings.addString(NAME, m_name);
        settings.addString(DESCRIPTION, m_description);
        settings.addString(VERSION, m_version);
        final NodeSettingsWO snippet = settings.addNodeSettings(SNIPPET);
        m_snippetSettings.saveSettings(snippet);
    }

    /**
     * Loads parameters.
     *
     * @param settings to load from
     */
    public void loadSettings(final NodeSettingsRO settings) {
        try {
            final String metaCategory = settings.getString(META_CATEGORY, null);
            m_metaCategory = metaCategory != null ? metaCategory : RSnippetTemplate.class.getName();
            m_category = settings.getString(CATEGORY, "default");
            m_name = settings.getString(NAME, "?");
            m_description = settings.getString(DESCRIPTION, "");
            m_version = settings.getString(m_version, RSnippetTemplate.VERSION_1_X);
            final NodeSettingsRO snippet = settings.getNodeSettings(SNIPPET);
            m_snippetSettings = new RSnippetSettings();
            m_snippetSettings.loadSettingsForDialog(snippet);
            m_uuid = m_snippetSettings.getTemplateUUID();
        } catch (final InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((m_uuid == null) ? 0 : m_uuid.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RSnippetTemplate other = (RSnippetTemplate)obj;
        if (m_uuid == null) {
            if (other.m_uuid != null) {
                return false;
            }
        } else if (!m_uuid.equals(other.m_uuid)) {
            return false;
        }
        return true;
    }

}
