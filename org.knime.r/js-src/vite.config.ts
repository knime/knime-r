// eslint-disable-next-line spaced-comment
/// <reference types="vitest" />
import { URL, fileURLToPath } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin from "vite-plugin-monaco-editor-esm";
import svgLoader from "vite-svg-loader";

// @ts-expect-error svgo.config is not typed
import { svgoConfig } from "@knime/styles/config/svgo.config";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    monacoEditorPlugin({ languageWorkers: ["editorWorkerService"] }),
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
  server: {
    headers: {
      // Disable the cache
      "Cache-Control": "no-cache, no-store, must-revalidate",
      Pragma: "no-cache",
      Expires: "0",

      // Satisfy KNIME's strict iframe policies so the connection is actually allowed
      "Cross-Origin-Embedder-Policy": "credentialless",
      "Cross-Origin-Resource-Policy": "cross-origin",
    },
  },
  test: {
    setupFiles: [
      fileURLToPath(new URL("./src/test-setup/setup.ts", import.meta.url)),
    ],
    include: ["src/**/__tests__/**/*.test.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
    exclude: ["**/node_modules/**", "**/dist/**"],
    environment: "jsdom",
    reporters: ["default"],
    root: fileURLToPath(new URL("./", import.meta.url)),
    server: {
      deps: { inline: ["@knime/scripting-editor", "@knime/kds-components"] },
    },
    pool: "threads",
    alias: {
      "monaco-editor": fileURLToPath(
        new URL("./src/__mocks__/monaco-editor", import.meta.url),
      ),
    },
    coverage: {
      provider: "v8",
      all: true,
      exclude: [
        "coverage/**",
        "dist/**",
        "**/*.d.ts",
        "**/__tests__/**",
        "**/__mocks__/**",
        "src/test-setup/**",
        "**/{vite,vitest,postcss,eslint,stylelint}.config.{js,cjs,mjs,ts}",
        "**/.prettierrc.{js,cjs,yml}",
        "src/main.ts",
        "src/init.ts",
      ],
      reporter: ["html", "text", "lcov"],
    },
  },
});
