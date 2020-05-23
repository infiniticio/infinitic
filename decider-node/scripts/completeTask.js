const { pulsar } = require('../pulsar');
const { taskEngineMessageType } = require('../avro');

const taskId = '563b520d-f0b1-458f-a49f-4d8e8274885f';
const taskAttemptId = '967fee3c-36ad-42f3-9cc9-7de72343e9b5';
const taskAttemptIndex = 1;

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
  m.taskAttemptId = taskAttemptId
  m.taskAttemptIndex = taskAttemptIndex
  m.output = { "bytes": Buffer.from('def') }

  var msg = new Object()
  msg.type = "TaskAttemptCompleted"
  msg.taskId = m.taskId
  msg[msg.type] = {"com.zenaton.taskmanager.messages.engine.AvroTaskAttemptCompleted": m}

  // Send message
  producer.send({data: taskEngineMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();