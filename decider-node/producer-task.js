const { pulsar } = require('./pulsar');
const { v4: uuidv4 } = require('uuid');
const { taskDispatchedType, taskMessageType } = require('./avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var atd = new taskDispatchedType.getRecordConstructor()
  atd.taskId = uuidv4()
  atd.sentAt = 1588705988
  atd.taskName = "MyTask"
  atd.taskData = null //{ "bytes": Buffer.from('abc') }
  atd.workflowId = { "string": uuidv4() }

  var atm = new taskMessageType.getRecordConstructor()
  atm.type = "TaskDispatched"
  atm.msg = {'com.zenaton.messages.topics.tasks.AvroTaskDispatched': atd}

  // Send messages
  producer.send({data: taskMessageType.toBuffer(atm)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();