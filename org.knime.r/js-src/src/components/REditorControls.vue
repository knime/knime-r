<script setup lang="ts">
import { computed } from "vue";

import { Button, LoadingIcon } from "@knime/components";
import { editor } from "@knime/scripting-editor";
import CancelIcon from "@knime/styles/img/icons/circle-close.svg";
import PlayIcon from "@knime/styles/img/icons/play.svg";
import ReloadIcon from "@knime/styles/img/icons/reload.svg";

import { rScriptingService } from "@/r-scripting-service";
import { useSessionStatusStore } from "@/store";

const sessionStatus = useSessionStatusStore();
const mainEditorState = editor.useMainCodeEditorStore();

const running = computed(
  () =>
    sessionStatus.status === "RUNNING_ALL" ||
    sessionStatus.status === "RUNNING_SELECTED",
);

const runningAll = computed(() => sessionStatus.status === "RUNNING_ALL");
const runningSelected = computed(
  () => sessionStatus.status === "RUNNING_SELECTED",
);

const hasSelection = computed(
  () => (mainEditorState.value?.selection.value ?? "") !== "",
);

const runAllButtonText = computed(() => {
  if (runningAll.value) {
    return "Running...";
  }
  return "Run all";
});

const runSelectedButtonText = computed(() => {
  if (runningSelected.value) {
    return "Running...";
  }
  return "Run selected lines";
});

const runAllClicked = () => {
  if (runningAll.value) {
    rScriptingService.killSession();
  } else {
    rScriptingService.runScript();
  }
};

const runSelectedClicked = () => {
  if (runningSelected.value) {
    rScriptingService.killSession();
  } else {
    rScriptingService.runSelectedLines();
  }
};
</script>

<template>
  <div class="r-editor-controls">
    <Button
      compact
      with-border
      :disabled="(running && !runningSelected) || !hasSelection"
      class="run-selected-button"
      title="Run selected lines"
      @click="runSelectedClicked"
    >
      <div>
        <PlayIcon v-if="!runningSelected" />
        <LoadingIcon v-else class="spinning" />
        <CancelIcon v-if="runningSelected" />
        {{ runSelectedButtonText }}
      </div>
    </Button>
    <Button
      primary
      compact
      :disabled="running && !runningAll"
      class="run-all-button"
      title="Run all – Shift Enter"
      @click="runAllClicked"
    >
      <div>
        <PlayIcon v-if="!runningAll" />
        <LoadingIcon v-else class="spinning" />
        <CancelIcon v-if="runningAll" />
        {{ runAllButtonText }}
      </div>
    </Button>
    <Button
      compact
      with-border
      :disabled="running"
      class="reset-button"
      title="Reset workspace"
      @click="rScriptingService.resetWorkspace()"
    >
      <div>
        <ReloadIcon />
        Reset
      </div>
    </Button>
  </div>
</template>

<style scoped lang="postcss">
.r-editor-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.run-selected-button {
  width: 180px;
}

.run-all-button {
  width: 140px;
}

.reset-button {
  width: 100px;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
