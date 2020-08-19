const copy = require('rollup-plugin-copy');

module.exports = {
  rollup(config, options) {
    config.plugins.push(
      copy({
        targets: [
          {
            src: 'src/avro/taskManager/messages/envelopes/*.avsc',
            dest: 'dist/avro/taskManager/messages/envelopes',
          },
          {
            src: 'src/avro/taskManager/messages/*.avsc',
            dest: 'dist/avro/taskManager/messages',
          },
          {
            src: 'src/avro/common/data/*.avsc',
            dest: 'dist/avro/common/data',
          },
        ],
      })
    );

    return config;
  }
};
