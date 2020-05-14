const { pulsar } = require('../pulsar');
const { taskMessageType } = require('../avro');

const taskId = '7469c66c-2938-4ba0-84af-144f59f3ced4';
const taskAttemptId = 'd79cceac-86e6-4a0d-b059-8c012c90f722';
const taskAttemptIndex = 0;

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
  msg[msg.type] = {"com.zenaton.taskmanager.messages.events.AvroTaskAttemptCompleted": m}

  // Send message
  producer.send({data: taskMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();