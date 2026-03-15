import "vitest-canvas-mock";
import { vi } from "vitest";
import { LogLevels, createConsola } from "consola";

import { initMocked } from "@knime/scripting-editor";
import { DEFAULT_INITIAL_DATA } from "@knime/scripting-editor/initial-data-service-browser-mock";

import { R_INITIAL_SETTINGS } from "@/__mocks__/initial-settings";

window.consola = createConsola({
  level: LogLevels.log,
});

// NB: We do not use importActual here, because we want to ensure that no original code of
// @knime/ui-extension-service is used. The original code could cause unexpected timeouts on
// `getConfig`.
vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: {
    getInstance: vi.fn(() =>
      Promise.resolve({
        baseService: {
          getConfig: vi.fn(() =>
            Promise.resolve({
              nodeId: "nodeId",
              projectId: "projectId",
              workflowId: "workflowId",
              resourceInfo: {
                baseUrl: "http://localhost/",
                path: "something/something/someFile.html",
              },
            }),
          ),
          getResourceLocation: vi.fn(() => "someResourceLocation"),
        },
      }),
    ),
  },
  DefaultSettingComparator: class DefaultSettingComparator {},
}));

// Initialize @knime/scripting-editor with mock data
initMocked({
  scriptingService: {
    sendToService: vi.fn(() => Promise.resolve(undefined)),
    callRpcMethod: vi.fn(),
    getOutputPreviewTableInitialData: vi.fn(() => Promise.resolve(undefined)),
    registerEventHandler: vi.fn(),
    connectToLanguageServer: vi.fn(() => Promise.resolve()),
    isKaiEnabled: vi.fn(),
    isLoggedIntoHub: vi.fn(),
    getAiDisclaimer: vi.fn(),
    getAiUsage: vi.fn(),
    isCallKnimeUiApiAvailable: vi.fn(() => Promise.resolve(false)),
    sendAlert: vi.fn(),
  },
  settingsService: {
    getSettings: vi.fn(),
    getSettingsInitialData: vi.fn(() => R_INITIAL_SETTINGS),
    registerSettingsGetterForApply: vi.fn(),
    registerSettings: vi.fn(() => vi.fn()),
  },
  initialData: DEFAULT_INITIAL_DATA,
  displayMode: "small",
});
