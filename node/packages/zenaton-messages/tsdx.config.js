const copy = require('rollup-plugin-copy');

module.exports = {
  rollup(config, options) {
    config.plugins.push(
      copy({
        targets: [
          {
            src: 'src/avro/messages/envelopes/*.avsc',
            dest: 'dist/avro/messages/envelopes',
          },
          {
            src: 'src/avro/messages/*.avsc',
            dest: 'dist/avro/messages',
          },
        ],
      })
    );

    return config;
  },
};
