const { pulsar } = require('./pulsar');
const { forMonitoringPerNameMessage } = require('./avro');

(async () => {
  // Create a consumer
  const consumer = await pulsar.subscribe({
    topic: 'persistent://public/default/monitoring-per-name',
    subscription: 'subMonitoringPerNameMessage',
    subscriptionType: 'Shared',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    console.log(forMonitoringPerNameMessage.fromBuffer(msg.getData()))
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await pulsar.close();
})();