<script setup lang="ts">
import { ref } from "vue";

import { FunctionButton } from "@knime/components";
import {
  type ConsoleHandler,
  type GenericNodeSettings,
  InputOutputPane,
  OutputConsole,
  ScriptingEditor,
  consoleHandler,
  getInitialData,
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
