const copy = require('rollup-plugin-copy');

module.exports = {
  rollup(config, options) {
    config.plugins.push(
      copy({
        targets: [
          {
            src: 'src/avro/messages/engine/*',
            dest: 'dist/avro/messages/engine',
          },
          {
            src: 'src/avro/messages/workers/*',
            dest: 'dist/avro/messages/workers',
          },
        ],
      })
    );

    return config;
  },
};
