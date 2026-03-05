import { createApp } from "vue";
import { Consola, LogLevels } from "consola";

import { useKdsLegacyMode } from "@knime/kds-components";
import { init } from "@knime/scripting-editor";

import App from "@/components/App.vue";

import { initR } from "./init";

const setupConsola = () => {
  const consola = new Consola({
    level: import.meta.env.DEV ? LogLevels.trace : LogLevels.error,
  });
  const globalObject = typeof global === "object" ? global : window;
  // @ts-expect-error consola is expected as a global by @knime/utils
  globalObject.consola = consola;
};

setupConsola();
useKdsLegacyMode(true);

await init();
initR();

createApp(App).mount("#app");
