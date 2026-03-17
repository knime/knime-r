import { editor, getScriptingService } from "@knime/scripting-editor";

import { type ExecutionStatus, useSessionStatusStore } from "./store";

type ExecutionFinishedInfo = {
  status: ExecutionStatus;
  message?: string;
};

const sessionStatus = useSessionStatusStore();
const mainEditorState = editor.useMainCodeEditorStore();

export const rScriptingService = {
  /** Runs the full script in a new R session. The old session (if any) is closed first. */
  runScript: (): void => {
    const script = mainEditorState.value?.text.value ?? "";
    getScriptingService().sendToService("runScript", [script]);
    sessionStatus.status = "RUNNING_ALL";
  },

  /** Runs the selected lines (or current line) in the existing R session. */
  runSelectedLines: (): void => {
    const selectedLines = mainEditorState.value?.selectedLines.value ?? "";
    getScriptingService().sendToService("runInExistingSession", [
      selectedLines,
    ]);
    sessionStatus.status = "RUNNING_SELECTED";
  },

  /** Kills the running R session. */
  killSession: (): void => {
    getScriptingService().sendToService("killSession");
  },

  /** Resets the R workspace by clearing all variables and re-importing input data. */
  resetWorkspace: (): void => {
    getScriptingService().sendToService("resetWorkspace");
    sessionStatus.status = "RUNNING_ALL";
  },
};

/** Registers all event handlers for R scripting events. Must be called once on startup. */
export const initREventHandlers = (): void => {
  getScriptingService().registerEventHandler(
    "r-execution-finished",
    (info: ExecutionFinishedInfo) => {
      sessionStatus.status = "IDLE";
      sessionStatus.lastExecutionStatus = info.status;
    },
  );

  getScriptingService().registerEventHandler("r-plot", (base64Png: string) => {
    sessionStatus.latestPlotData = base64Png;
  });
};
