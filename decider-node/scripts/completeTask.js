const { pulsar } = require('../pulsar');
const { taskMessageType } = require('../avro');

const taskId = '0957bf99-4183-42a8-aaea-e0086b3744f3';
const taskAttemptId = '1faadefc-e106-4cb2-8b73-91e68356e3f5';
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
  msg.TaskAttemptCompleted = {"com.zenaton.taskmanager.messages.events.AvroTaskAttemptCompleted": m}

  // Send message
  producer.send({data: taskMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();