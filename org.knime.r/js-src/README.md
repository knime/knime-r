# R Scripting Editor

The scripting editor dialog for the R nodes.

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Project Setup

```sh
npm install
```

### KNIME Dialog Development Mode

Start KNIME Analytics Platform with the arguments

```
-Dorg.knime.ui.dev.mode=true
-Dorg.knime.ui.dev.node.dialog.url=http://localhost:5173/
```

Run the development server

```sh
npm run dev:knime
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

Note: noisy Vue warnings are suppressed if you set the environment flag `SUPPRESS_WARNINGS=true`,
and they are also suppressed when running with `CI=true` which is set by default in most pipeline
environments.

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
