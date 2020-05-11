const { pulsar } = require('./pulsar');
const { taskMessageType } = require('./avro');

(async () => {
  // Create a consumer
  const consumer = await pulsar.subscribe({
    topic: 'persistent://public/default/tasks',
    subscription: 'subTasks',
    subscriptionType: 'Exclusive',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    // console.log(Object.values(taskMessageType.fromBuffer(msg.getData()).msg)[0])
    console.log(taskMessageType.fromBuffer(msg.getData()))
   consumer.acknowledge(msg);
  }

  await consumer.close();
  await pulsar.close();
})();