const Pulsar = require('pulsar-client');

// Create a client
module.exports.pulsar = new Pulsar.Client({
  serviceUrl: 'pulsar://localhost:6650',
  operationTimeoutSeconds: 30,
});