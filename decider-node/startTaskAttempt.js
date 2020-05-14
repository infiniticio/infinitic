const { pulsar } = require('./pulsar');
const { taskAttemptStartedType, taskMessageType } = require('./avro');

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

  var tas = new taskAttemptStartedType.getRecordConstructor()
  tas.taskId = taskId
  tas.sentAt = 1588705988
  tas.taskAttemptId = taskAttemptId
  tas.taskAttemptIndex = taskAttemptIndex
  tas.taskAttemptDelayBeforeRetry = 10.0
  tas.taskAttemptDelayBeforeTimeout = 3.0

  var atm = new taskMessageType.getRecordConstructor()
  atm.type = "TaskAttemptStarted"
  atm.taskId = tas.taskId
  atm.TaskAttemptStarted = {'com.zenaton.taskmanager.messages.events.AvroTaskAttemptStarted': tas}

  // Send taskDispatched messages
  producer.send({data: taskMessageType.toBuffer(atm)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();