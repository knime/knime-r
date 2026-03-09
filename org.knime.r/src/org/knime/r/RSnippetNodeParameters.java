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
package org.knime.r;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.r.bin.preferences.RPreferenceInitializer;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for the R Snippet node WebUI dialog.
 *
 * @author KNIME GmbH
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class RSnippetNodeParameters implements NodeParameters {

    // script field (no @Widget - binds to code editor, not settings panel)

    @Persist(configKey = "script")
    String m_script = "";

    // internal version marker, no @Widget
    @Persist(configKey = "version")
    String m_version = RSnippet.VERSION_1_X;

    // Status messages - shown before all settings panels to surface R configuration problems

    @TextMessage(NoRFoundMessageProvider.class)
    Void m_noRFoundMessage;

    @TextMessage(NoLspFoundMessageProvider.class)
    Void m_noLspFoundMessage;

    // sections

    @Section(title = "Advanced")
    interface AdvancedSection {
    }

    @Section(title = "R Home")
    interface RHomeSection {
    }

    // Advanced section fields

    @Widget(title = "Treat NaN, Inf and -Inf as missing values",
        description = "Convert NaN and +/-Infinity output generated by R to missing cells. "
            + "Enable this option for backwards compatibility with the behavior before KNIME version 2.10.")
    @Persist(configKey = "Output non numbers (NaN, Inf, -Inf) as missing cells")
    @Layout(AdvancedSection.class)
    boolean m_outNonNumbersAsMissing = false;

    @Widget(title = "Send row names of input table to R",
        description = "Whether to send the row names of the KNIME input table to R. "
            + "Disabling this option can improve performance with very large tables. "
            + "When disabled, row names of knime.in will be the default R row names: 1:n.")
    @Persist(configKey = "sendRowNames")
    @Layout(AdvancedSection.class)
    boolean m_sendRowNames = true;

    @Widget(title = "R type for knime.in",
        description = "Controls the R type used for the knime.in variable. "
            + "By default knime.in is a data.frame. "
            + "For large input tables, using data.table is likely more memory-efficient "
            + "and may therefore be faster. "
            + "Note: support for data.table is experimental and requires the data.table R package.")
    @Persistor(KnimeInTypePersistor.class)
    @Layout(AdvancedSection.class)
    KnimeInType m_knimeInType = KnimeInType.DATA_FRAME;

    @Widget(title = "Send batch size",
        description = "The input of the R Snippet node is sent in row batches (default: 10000 rows at a time). "
            + "This value controls how many rows KNIME retains in memory at any given time during the transfer. "
            + "For large tables, a higher batch size may be faster but will require additional memory.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = "sendBatchSize")
    @Layout(AdvancedSection.class)
    int m_sendBatchSize = 10000;

    // R Home section predicates and refs

    static final class OverwriteRHomeRef implements ParameterReference<Boolean> {
    }

    static final class UseRHomePathRef implements ParameterReference<Boolean> {
    }

    static final class IsOverwriteRHome implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(OverwriteRHomeRef.class).isTrue();
        }
    }

    static final class IsOverwriteAndUsePath implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(OverwriteRHomeRef.class).isTrue()
                .and(i.getBoolean(UseRHomePathRef.class).isTrue());
        }
    }

    static final class IsOverwriteAndUseCondaVar implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(OverwriteRHomeRef.class).isTrue()
                .and(i.getBoolean(UseRHomePathRef.class).isFalse());
        }
    }

    // R Home section fields

    @Widget(title = "Overwrite default path to R home",
        description = "Check this option to select a path to the home of an R installation different from the "
            + "path configured on the R preference page. The path must point to the root folder of the R "
            + "installation tree. You can either specify the path directly or use the R installation from a "
            + "conda environment.")
    @ValueReference(OverwriteRHomeRef.class)
    @Persist(configKey = "overwriteRHome")
    @Layout(RHomeSection.class)
    boolean m_overwriteRHome = false;

    @Widget(title = "Use R home path",
        description = "When enabled, specify the R home directory by an explicit file system path. "
            + "When disabled, specify the R home via a conda environment flow variable.")
    @Effect(predicate = IsOverwriteRHome.class, type = EffectType.SHOW)
    @ValueReference(UseRHomePathRef.class)
    @Persist(configKey = "useRPathHome")
    @Layout(RHomeSection.class)
    boolean m_useRHomePath = true;

    @Widget(title = "R home path",
        description = "Path to the root folder of the R installation. Must point to a valid R installation directory.")
    @Effect(predicate = IsOverwriteAndUsePath.class, type = EffectType.SHOW)
    @Persist(configKey = RSnippetSettings.R_HOME_PATH)
    @Layout(RHomeSection.class)
    String m_rHomePath = "";

    @Widget(title = "Conda environment variable for R home",
        description = "Name of a flow variable that holds the path to the conda environment containing the "
            + "R installation to use. Such a flow variable can be created using a Conda Environment Propagation node.")
    @Effect(predicate = IsOverwriteAndUseCondaVar.class, type = EffectType.SHOW)
    @Persist(configKey = RSnippetSettings.R_HOME_VARIABLE)
    @Layout(RHomeSection.class)
    String m_condaVariableName = "";

    // TextMessage providers

    /**
     * Shows an ERROR message when no R installation is configured in KNIME Preferences.
     * R is required both for node execution (via Rserve) and for autocompletion (via languageserver).
     */
    static final class NoRFoundMessageProvider implements TextMessage.SimpleTextMessageProvider {

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            final String rscriptPath = RPreferenceInitializer.getRProvider().getRBinPath("Rscript");
            return rscriptPath == null || rscriptPath.isBlank();
        }

        @Override
        public String title() {
            return "R is not configured";
        }

        @Override
        public String description() {
            return "No R installation was found. "
                + "Install R (https://www.r-project.org) and set the R home path in "
                + "KNIME Preferences \u2192 KNIME \u2192 R. "
                + "Rserve is required for node execution; install it in R with: "
                + "install.packages(\"Rserve\")";
        }

        @Override
        public MessageType type() {
            return MessageType.ERROR;
        }
    }

    /**
     * Shows an INFO message when R is configured but the {@code languageserver} package is not installed.
     * The node still executes without the language server; only autocompletion and hover are disabled.
     */
    static final class NoLspFoundMessageProvider implements TextMessage.SimpleTextMessageProvider {

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            final String rscriptPath = RPreferenceInitializer.getRProvider().getRBinPath("Rscript");
            if (rscriptPath == null || rscriptPath.isBlank()) {
                return false; // the NoRFoundMessage already covers this case
            }
            try {
                final Process check = new ProcessBuilder(rscriptPath, "--no-save", "--no-restore", "--slave",
                    "-e", "if (!requireNamespace('languageserver', quietly=TRUE)) quit(status=1)")
                    .redirectErrorStream(true)
                    .start();
                // Drain stdout to prevent the process from blocking on a full pipe buffer
                check.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                final boolean timedOut = !check.waitFor(5, TimeUnit.SECONDS);
                if (timedOut) {
                    check.destroyForcibly();
                    return false; // cannot determine — don't show a potentially misleading message
                }
                return check.exitValue() != 0;
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }
        }

        @Override
        public String title() {
            return "Language server not installed";
        }

        @Override
        public String description() {
            return "The 'languageserver' R package is not installed. "
                + "Autocompletion and hover support are disabled. "
                + "To enable them, run in R: install.packages(\"languageserver\")";
        }

        @Override
        public MessageType type() {
            return MessageType.INFO;
        }
    }

    // KnimeInType enum

    enum KnimeInType {
        @Label(value = "data.frame", description = "Use a standard R data.frame for knime.in (default).")
        DATA_FRAME,

        @Label(value = "data.table (experimental)",
            description = "Use a data.table for knime.in. May be more memory-efficient for large tables. "
                + "Requires the data.table R package. Experimental.")
        DATA_TABLE;
    }

    // KnimeInType persistor

    static final class KnimeInTypePersistor implements NodeParametersPersistor<KnimeInType> {

        @Override
        public KnimeInType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String type = settings.getString("knimeInType", "data.frame");
            return "data.table".equals(type) ? KnimeInType.DATA_TABLE : KnimeInType.DATA_FRAME;
        }

        @Override
        public void save(final KnimeInType obj, final NodeSettingsWO settings) {
            settings.addString("knimeInType", obj == KnimeInType.DATA_TABLE ? "data.table" : "data.frame");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"knimeInType"}};
        }
    }
}
