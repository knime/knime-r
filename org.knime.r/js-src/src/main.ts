import { createApp } from "vue";
import { LogLevels, createConsola } from "consola";

import { useKdsLegacyMode } from "@knime/kds-components";
import { init } from "@knime/scripting-editor";

import App from "@/components/App.vue";

import { initREventHandlers } from "./r-scripting-service";

// Setup global consola instance
window.consola = createConsola({
  level: import.meta.env.DEV ? LogLevels.trace : LogLevels.error,
});

useKdsLegacyMode(true);

await init();
initREventHandlers();

createApp(App).mount("#app");
