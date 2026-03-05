// eslint-disable-next-line @typescript-eslint/no-require-imports
const { preset } = require("@knime/styles/config/postcss.config.cjs");

module.exports = {
  plugins: {
    "postcss-mixins": {},
    "postcss-preset-env": preset,
  },
};
