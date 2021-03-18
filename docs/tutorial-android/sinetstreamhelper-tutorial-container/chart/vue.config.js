const CompressionPlugin = require('compression-webpack-plugin');

module.exports = {
  transpileDependencies: [
    'vuetify',
  ],

  chainWebpack: (config) => {
    config.plugins.delete('prefetch');
    config.plugin('CompressionPlugin').use(CompressionPlugin);
  },
};
