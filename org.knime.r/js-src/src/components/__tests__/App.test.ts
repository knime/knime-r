import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import { InputOutputPane, ScriptingEditor } from "@knime/scripting-editor";
import { NodeParametersPanel } from "@knime/scripting-editor/parameters";

import App from "../App.vue";

// OutputConsole uses xterm.js which calls window.matchMedia — not available in jsdom
const stubs = { OutputConsole: true };

describe("App", () => {
  const doMount = () => mount(App, { global: { stubs } });

  it("renders the scripting editor", () => {
    const wrapper = doMount();
    expect(wrapper.findComponent(ScriptingEditor).exists()).toBe(true);
  });

  it("renders the input output pane", () => {
    const wrapper = doMount();
    expect(wrapper.findComponent(InputOutputPane).exists()).toBe(true);
  });

  it("renders the node parameters panel", () => {
    const wrapper = doMount();
    expect(wrapper.findComponent(NodeParametersPanel).exists()).toBe(true);
  });
});
