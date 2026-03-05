import { vi } from "vitest";

export const MarkerTag = {};
export const MarkerSeverity = {};
export const languages = {
  CompletionItemKind: { Constant: 1 },
  CompletionItemInsertTextRule: { InsertAsSnippet: 0 },
  registerCompletionItemProvider: vi.fn(),
};

export const editor = {
  getModel: vi.fn(() => ({
    getValue: () => "foo",
    setValue: vi.fn(),
    isAttachedToEditor: () => false,
    onDidChangeContent: vi.fn(),
    updateOptions: vi.fn(),
    dispose: vi.fn(),
    setEOL: vi.fn(),
  })),
  onDidChangeModelContent: vi.fn(),
  createModel: vi.fn(),
  create: vi.fn(() => ({
    onDidChangeCursorSelection: vi.fn(),
    onDidPaste: vi.fn(),
    dispose: vi.fn(),
    updateOptions: vi.fn(),
    getOptions: vi.fn(() => ({
      get: vi.fn(),
    })),
    onDidChangeModelContent: vi.fn(),
    getContentHeight: vi.fn(),
    focus: vi.fn(),
    layout: vi.fn(),
    getDomNode: vi.fn(),
  })),
  defineTheme: vi.fn(),
  setTheme: vi.fn(),
  EndOfLineSequence: { LF: 1, CRLF: 2 },
};
export const Uri = {
  parse: vi.fn(),
};
