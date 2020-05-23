const { pulsar } = require('../pulsar');
const { taskEngineMessageType } = require('../avro');

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
  msg[msg.type] = {"com.zenaton.taskmanager.messages.engine.AvroRetryTask": m}

  // Send message
  producer.send({data: taskEngineMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();