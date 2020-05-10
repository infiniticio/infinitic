const { pulsar } = require('./pulsar');
const { taskAttemptDispatchedType } = require('./avro');

const name = 'myTask';

(async () => {
  // Create a consumer
  const consumer = await pulsar.subscribe({
    topic: `persistent://public/default/tasks-${name}`,
    subscription: 'subTaskAttempts-${name}',
    subscriptionType: 'Exclusive',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    console.log(Object.values(taskAttemptDispatchedType.fromBuffer(msg.getData()).msg)[0])
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await pulsar.close();
})();