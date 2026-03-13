import { reactive } from "vue";

export type SessionStatus = "IDLE" | "RUNNING_ALL" | "RUNNING_SELECTED";

export type ExecutionStatus = "SUCCESS" | "ERROR" | "CANCELLED";

export type SessionStatusStore = {
  status: SessionStatus;
  lastExecutionStatus?: ExecutionStatus;
};

const sessionStatus: SessionStatusStore = reactive<SessionStatusStore>({
  status: "IDLE",
});

export const useSessionStatusStore = (): SessionStatusStore => sessionStatus;
