import type { SettingsInitialData } from "@knime/scripting-editor";

/**
 * Minimal mock settings initial data for the R scripting node.
 * Provides the minimal structure needed for NodeParametersPanel to render in tests.
 */
export const R_INITIAL_SETTINGS: SettingsInitialData = {
  data: {
    model: {},
  },
  schema: {
    type: "object",
    properties: {
      model: {
        type: "object",
        properties: {},
      },
    },
  },
  // eslint-disable-next-line camelcase
  ui_schema: {
    elements: [],
  },
  flowVariableSettings: {},
  persist: {
    type: "object",
    properties: {
      model: {
        type: "object",
        properties: {},
      },
    },
  },
};
