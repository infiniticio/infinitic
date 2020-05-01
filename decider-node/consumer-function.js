const Pulsar = require('pulsar-client');

(async () => {
  // Create a client
  const client = new Pulsar.Client({
    serviceUrl: 'pulsar://localhost:6650',
    operationTimeoutSeconds: 30,
  });

  // Create a consumer
  const consumer = await client.subscribe({
    topic: 'persistent://public/default/workflows',
    subscription: 'public/default/TraceFunction',
    subscriptionType: 'KeyShared',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 100; i += 1) {
    const msg = await consumer.receive();
    console.log(msg.getData().toString());
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await client.close();
})();