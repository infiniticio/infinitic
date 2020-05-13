const { pulsar } = require('./pulsar');
const { taskAttemptStartedType, taskMessageType } = require('./avro');

const taskId = 'c6484107-c191-4581-b318-bf970ae357da';
const taskAttemptId = '120c61ca-c866-4e68-94ec-5fe6e8d0fa0a';
const taskAttemptIndex = 10;

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
  atd.taskAttemptDelayBeforeRetry = 15.0
  atd.taskAttemptDelayBeforeTimeout = 30.0

  var atm = new taskMessageType.getRecordConstructor()
  atm.type = "TaskAttemptStarted"
  atm.taskId = atd.taskId
  atm.taskAttemptStarted = {'com.zenaton.taskmanager.messages.AvroTaskAttemptStarted': atd}

  // Send taskDispatched messages
  producer.send({data: taskMessageType.toBuffer(atm)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();