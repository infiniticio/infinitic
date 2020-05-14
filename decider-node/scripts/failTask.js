const { pulsar } = require('../pulsar');
const { taskMessageType } = require('../avro');

const taskId = '15a243f6-5477-4dd7-9ebe-c89d8ad6744c';
const taskAttemptId ='9de5518b-e9c3-4265-ae72-cbe8ae69bfbd';
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
  m.taskAttemptError = Buffer.from('Error!')
  m.taskAttemptDelayBeforeRetry = 5.0

  var msg = new Object()
  msg.type = "TaskAttemptFailed"
  msg.taskId = m.taskId
  msg.TaskAttemptFailed = {"com.zenaton.taskmanager.messages.events.AvroTaskAttemptFailed": m}

  // Send message
  producer.send({data: taskMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();