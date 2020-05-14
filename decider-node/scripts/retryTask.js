const { pulsar } = require('../pulsar');
const { taskMessageType } = require('../avro');

const taskId = '15a243f6-5477-4dd7-9ebe-c89d8ad6744c';

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.taskId = taskId
  m.sentAt = 1588705988

  var msg = new Object()
  msg.type = "RetryTask"
  msg.taskId = m.taskId
  msg.RetryTask = {"com.zenaton.taskmanager.messages.commands.AvroRetryTask": m}

  // Send message
  producer.send({data: taskMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();