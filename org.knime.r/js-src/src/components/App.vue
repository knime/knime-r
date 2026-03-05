<script setup lang="ts">
import { ref } from "vue";
import * as monaco from "monaco-editor";

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
  initConsoleEventHandler,
  joinSettings,
  setConsoleHandler,
} from "@knime/scripting-editor";
import { NodeParametersPanel } from "@knime/scripting-editor/parameters";
import TrashIcon from "@knime/styles/img/icons/trash.svg";

import REditorControls from "@/components/REditorControls.vue";

const initialData = getInitialData();

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
const mainEditor = editor.useMainCodeEditorStore();
mainEditor.value?.editorModel.setEOL(monaco.editor.EndOfLineSequence.LF);
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
