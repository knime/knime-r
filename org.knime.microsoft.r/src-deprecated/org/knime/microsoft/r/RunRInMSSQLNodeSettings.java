/*
 * ------------------------------------------------------------------
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
 */
package org.knime.microsoft.r;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node Settings for the RunRInMSSQLNodeModel.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 *
 */
final class RunRInMSSQLNodeSettings {

    /** Settings key used to store the output table name under. */
    public static final String KEY_OUTPUT_TABLE_NAME = "OutputTableName";

    public static final String KEY_OVERWRITE_OUTPUT_TABLE = "OverwriteOutputTable";

    private final SettingsModelString m_outputTableNameModel =
        new SettingsModelString(KEY_OUTPUT_TABLE_NAME, "OutputTable");

    private final SettingsModelBoolean m_overwriteOutputTableModel =
        new SettingsModelBoolean(KEY_OVERWRITE_OUTPUT_TABLE, false);

    /**
     * Get name of the output sql table
     *
     * @return name of the table
     */
    public String getOutputTableName() {
        return m_outputTableNameModel.getStringValue();
    }

    /**
     * Set name of the output sql table
     *
     * @param outputTableName name for the table
     */
    public void setOutputTableName(final String outputTableName) {
        m_outputTableNameModel.setStringValue(outputTableName);
    }

    /**
     * Settings model for the name of the output sql table.
     *
     * @return the settings model
     */
    public SettingsModelString outputTableNameMode() {
        return m_outputTableNameModel;
    }

    /**
     * Set whether to overwrite the output sql table if it already exists.
     *
     * @return Whether to overwrite the output sql table if it already exists.
     */
    public boolean getOverwriteOutputTable() {
        return m_overwriteOutputTableModel.getBooleanValue();
    }

    /**
     * Set whether to overwrite the output sql table if it already exists.
     *
     * @param b Whether to overwrite the output sql table if it already exists.
     */
    public void setOverwriteOutputTable(final boolean b) {
        m_overwriteOutputTableModel.setBooleanValue(b);
    }

    /**
     * Settings model for whether to overwrite the output sql table
     *
     * @return the settings model
     */
    public SettingsModelBoolean overwriteOutputTableModel() {
        return m_overwriteOutputTableModel;
    }

    /**
     * Save settings
     *
     * @param settings Settings to save to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_outputTableNameModel.saveSettingsTo(settings);
        m_overwriteOutputTableModel.saveSettingsTo(settings);
    }

    /**
     * Load settings
     *
     * @param settings Settings to load from
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputTableNameModel.loadSettingsFrom(settings);
        m_overwriteOutputTableModel.loadSettingsFrom(settings);
    }

    /**
     * @param settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        try {
            m_outputTableNameModel.loadSettingsFrom(settings);
            m_overwriteOutputTableModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
        }
    }

}
