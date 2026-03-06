import { consoleHandler, getScriptingService } from "@knime/scripting-editor";

import { initREventHandlers } from "./r-scripting-service";
import { useSessionStatusStore } from "./store";

export const initR = (): void => {
  const sessionStatus = useSessionStatusStore();

  // Handle console output events from the R backend
  getScriptingService().registerEventHandler(
    "console",
    (data: { text: string; stderr: boolean }) => {
      if (data.stderr) {
        consoleHandler.writeln({ warning: data.text });
      } else {
        consoleHandler.writeln({ text: data.text });
      }
    },
  );

  // Handle execution-finished events (updates session status)
  initREventHandlers();

  // Initialise session status
  sessionStatus.status = "IDLE";

  // NOTE: Language server connection is initiated from App.vue via a watch() on the
  // Monaco editor model. connectToLanguageServer() must not be called here because the
  // editor model is not available until after the Vue app is mounted.
};
