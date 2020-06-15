const { pulsar } = require('./pulsar');
const { forWorkerMessage } = require('./avro');

const name = 'MyTask';

(async () => {
  const TOPIC = `tasks-workers-${name}`

  // Create a consumer
  const consumer = await pulsar.subscribe({
    topic: `persistent://public/default/${TOPIC}`,
    subscription: TOPIC,
    subscriptionType: 'Shared',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    console.log(forWorkerMessage.fromBuffer(msg.getData()))
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await pulsar.close();
})();