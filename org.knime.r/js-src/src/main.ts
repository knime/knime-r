import { createApp } from "vue";
import { LogLevels, createConsola } from "consola";

import { useKdsLegacyMode } from "@knime/kds-components";
import { init } from "@knime/scripting-editor";

import App from "@/components/App.vue";

// Setup global consola instance
window.consola = createConsola({
  level: import.meta.env.DEV ? LogLevels.trace : LogLevels.error,
});

// NOTE: For development, the legacy mode can be disabled and dark mode can be forced here
// const { currentMode } = useKdsDarkMode();
// currentMode.value = "dark"
useKdsLegacyMode(true);

await init();

createApp(App).mount("#app");
