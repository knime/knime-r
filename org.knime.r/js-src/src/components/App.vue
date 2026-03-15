<script setup lang="ts">
import { ref, watch, watchEffect } from "vue";
import * as monaco from "monaco-editor";

import { FunctionButton } from "@knime/components";
import {
  CompactTabBar,
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

// Set the end of line sequence to LF to ensure consistent behavior across different platforms.
const mainEditorState = editor.useMainCodeEditorStore();
watchEffect(() => {
  mainEditorState.value?.editorModel?.setEOL(
    monaco.editor.EndOfLineSequence.LF,
  );
});

// Right pane tabs
type RightPaneTab = "variables" | "settings" | "plot";
const rightPaneActiveTab = ref<RightPaneTab>("settings");
const rightPaneOptions = [
  { value: "variables", label: "Variables" },
  { value: "settings", label: "Settings" },
  { value: "plot", label: "Plot" },
];

// Connect to the R language server once the Monaco editor model is available.
// watch() is used (not initR()) because the editor model only exists after the
// Vue component tree is mounted — calling connectToLanguageServer() before that
// would throw "Editor model has not yet been initialized".
watch(
  () => mainEditorState.value?.editorModel,
  (editorModel) => {
    if (typeof editorModel === "undefined") {
      return;
    }
    getScriptingService()
      .sendToService("getRInfo")
      .then((info: string) => {
        consoleHandler.writeln({ text: `Using ${info}\n` });
      })
      .catch(() => {
        /* ignore */
      });
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
      ]"
    >
      <template #left-pane>
        <InputOutputPane :input-output-items="inputOutputItems" />
      </template>
      <template #right-pane>
        <div id="right-pane">
          <CompactTabBar
            v-model="rightPaneActiveTab"
            :possible-values="rightPaneOptions"
            name="rightPaneTabBar"
          />
          <div id="right-pane-content">
            <div v-show="rightPaneActiveTab === 'variables'" class="tab-placeholder">
              <!-- Variables: reserved for future use -->
            </div>
            <NodeParametersPanel
              v-show="rightPaneActiveTab === 'settings'"
              ref="nodeParametersPanel"
            />
            <div v-show="rightPaneActiveTab === 'plot'" class="plot-pane">
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
          </div>
        </div>
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
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");
@import url("@knime/kds-styles/kds-variables.css");
@import url("@knime/kds-styles/kds-legacy-theme.css");
</style>

<style scoped>
#right-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
}

#right-pane-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.tab-placeholder {
  height: 100%;
}

.plot-pane {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 8px;
  overflow: auto;
}

.plot-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.plot-empty {
  padding: 24px;
  font-style: italic;
  color: var(--knime-silver-sand-semi);
  text-align: center;
}
</style>
