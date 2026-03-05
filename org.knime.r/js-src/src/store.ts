import { reactive } from "vue";

export type SessionStatus = "IDLE" | "RUNNING_ALL" | "RUNNING_SELECTED";

export type ExecutionStatus = "SUCCESS" | "ERROR" | "CANCELLED";

export type SessionStatusStore = {
  status: SessionStatus;
  lastExecutionStatus?: ExecutionStatus;
  /** Base64-encoded PNG of the most recent plot, or null if no plot has been generated yet. */
  latestPlotData: string | null;
};

const sessionStatus: SessionStatusStore = reactive<SessionStatusStore>({
  status: "IDLE",
  latestPlotData: null,
});

export const useSessionStatusStore = (): SessionStatusStore => {
  return sessionStatus;
};
