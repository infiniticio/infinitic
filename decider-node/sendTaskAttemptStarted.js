const { pulsar } = require('./pulsar');
const { taskAttemptStartedType, taskMessageType } = require('./avro');

const taskId = 'e1d157c4-f056-45e5-bfad-41919c4cc907';
const taskAttemptId = '94863fa1-6fa5-4b8a-b376-484a941521fa';
const taskAttemptIndex = 8;

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var atd = new taskAttemptStartedType.getRecordConstructor()
  atd.taskId = taskId
  atd.sentAt = 1588705988
  atd.taskAttemptId = taskAttemptId
  atd.taskAttemptIndex = taskAttemptIndex
  atd.taskAttemptDelayBeforeRetry = 8.0
  atd.taskAttemptDelayBeforeTimeout = 8.0

  var atm = new taskMessageType.getRecordConstructor()
  atm.type = "TaskAttemptStarted"
  atm.taskId = atd.taskId
  atm.taskAttemptStarted = {'com.zenaton.messages.tasks.AvroTaskAttemptStarted': atd}

  // Send taskDispatched messages
  producer.send({data: taskMessageType.toBuffer(atm)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();