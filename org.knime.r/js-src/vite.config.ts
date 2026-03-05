import { URL, fileURLToPath } from "node:url";

import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin, {
  type IMonacoEditorOpts,
} from "vite-plugin-monaco-editor";
import svgLoader from "vite-svg-loader";
import { defineConfig } from "vite";

// @ts-expect-error svgo.config is not typed
import { svgoConfig } from "@knime/styles/config/svgo.config";

// Hack because default export of vite-plugin-monaco-editor is wrong
// https://github.com/vdesjs/vite-plugin-monaco-editor/issues/21
const monacoEditorPluginDefault = (monacoEditorPlugin as any).default as (
  options: IMonacoEditorOpts,
) => any;

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    monacoEditorPluginDefault({
      languageWorkers: ["editorWorkerService"],
    }),
    svgLoader({ svgoConfig }),
  ],
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  optimizeDeps: { exclude: ["@knime/scripting-editor"] },
  base: "./",
  build: {
    target: "esnext",
    outDir: "dist",
  },
});
