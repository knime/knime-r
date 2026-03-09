<script setup lang="ts">
import { ref, watch } from "vue";

import { FunctionButton } from "@knime/components";
import {
  type ConsoleHandler,
  type GenericNodeSettings,
  InputOutputPane,
  OutputConsole,
  ScriptingEditor,
  consoleHandler,
  editor,
  getInitialData,
  getScriptingService,
  initConsoleEventHandler,
  joinSettings,
  setConsoleHandler,
} from "@knime/scripting-editor";
import { NodeParametersPanel } from "@knime/scripting-editor/parameters";
import TrashIcon from "@knime/styles/img/icons/trash.svg";

import REditorControls from "@/components/REditorControls.vue";
import { useSessionStatusStore } from "@/store";

const initialData = getInitialData();
const sessionStore = useSessionStatusStore();
const mainEditorState = editor.useMainCodeEditorStore();

const inputOutputItems = [
  ...initialData.inputObjects,
  initialData.flowVariables,
];

const nodeParametersPanel = ref<InstanceType<
  typeof NodeParametersPanel
> | null>(null);

const toSettings = (commonSettings: GenericNodeSettings) =>
  joinSettings(
    commonSettings as { script: string },
    nodeParametersPanel.value?.getDataAndFlowVariableSettings(),
  );

// Connect to the R language server once the Monaco editor model is available.
// We use watch() here (not initR()) because the editor model only exists after the
// Vue component tree is mounted — calling connectToLanguageServer() before that
// throws "Editor model has not yet been initialized".
watch(
  () => mainEditorState.value?.editorModel,
  (editorModel) => {
    if (typeof editorModel === "undefined") {
      return;
    }
    // Print which R installation KNIME is using (diagnostic aid for the user).
    getScriptingService()
      .sendToService("getRInfo")
      .then((info: string) => {
        consoleHandler.writeln({ text: `Using ${info}\n` });
      })
      .catch(() => {
        /* ignore */
      });
    // Connect to the language server for live autocompletion and hover.
    consoleHandler.writeln({ text: "Connecting to R language server…\n" });
    getScriptingService()
      .connectToLanguageServer()
      .then(() => {
        consoleHandler.writeln({
          text:
            "R language server connected. Hover and autocompletion are active.\n" +
            "Note: autocompletion requires typing a partial identifier (e.g. 'pri') " +
            "before pressing Ctrl+Space.\n",
        });
      })
      .catch((e: Error) => {
        consoleHandler.writeln({
          warning:
            `R language server unavailable: ${e.message}\n` +
            "Install the 'languageserver' package to enable live autocompletion:\n" +
            "  install.packages('languageserver')\n",
        });
      });
  },
  { once: true },
);
</script>

<template>
  <main>
    <ScriptingEditor
      title="R Script"
      language="r"
      file-name="script.R"
      :to-settings="toSettings"
      :initial-pane-sizes="{
        left: 260,
        right: 380,
        bottom: 300,
      }"
      :additional-bottom-pane-tab-content="[
        {
          slotName: 'bottomPaneTabSlot:console',
          label: 'Console',
          associatedControlsSlotName: 'bottomPaneTabControlsSlot:console',
        },
        {
          slotName: 'bottomPaneTabSlot:plot',
          label: 'Plot',
        },
      ]"
    >
      <template #left-pane>
        <InputOutputPane :input-output-items="inputOutputItems" />
      </template>
      <template #right-pane>
        <NodeParametersPanel ref="nodeParametersPanel" />
      </template>
      <template #code-editor-controls>
        <REditorControls />
      </template>
      <template #bottomPaneTabSlot:console>
        <OutputConsole
          class="console"
          @console-created="
            (console: ConsoleHandler) => {
              setConsoleHandler(console);
              initConsoleEventHandler();
            }
          "
        />
      </template>
      <template #bottomPaneTabControlsSlot:console>
        <FunctionButton class="clear-button" @click="consoleHandler.clear">
          <TrashIcon />
        </FunctionButton>
      </template>
      <template #bottomPaneTabSlot:plot>
        <div class="plot-pane">
          <img
            v-if="sessionStore.latestPlotData"
            :src="'data:image/png;base64,' + sessionStore.latestPlotData"
            class="plot-image"
            alt="R plot"
          />
          <div v-else class="plot-empty">
            No plot yet. Run a script that calls <code>plot()</code>,
            <code>ggplot()</code>, or any other graphics function.
          </div>
        </div>
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");
@import url("@knime/kds-styles/kds-variables.css");
@import url("@knime/kds-styles/kds-legacy-theme.css");
</style>

<style scoped>
.plot-pane {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 8px;
  box-sizing: border-box;
}

.plot-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.plot-empty {
  color: var(--knime-silver-sand-semi);
  font-style: italic;
  text-align: center;
  padding: 24px;
}
</style>
