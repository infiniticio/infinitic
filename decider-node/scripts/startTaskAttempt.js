const { pulsar } = require('../pulsar');
const { taskMessageType } = require('../avro');

const taskId = '0957bf99-4183-42a8-aaea-e0086b3744f3';
const taskAttemptId = '1faadefc-e106-4cb2-8b73-91e68356e3f5';
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
  m.taskAttemptDelayBeforeRetry = 10.0
  m.taskAttemptDelayBeforeTimeout = 3.0

  var masg = new Object()
  masg.type = "TaskAttemptStarted"
  masg.taskId = m.taskId
  msg[msg.type] = {'com.zenaton.taskmanager.messages.events.AvroTaskAttemptStarted': m}

  // Send message
  producer.send({data: taskMessageType.toBuffer(masg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();