const { pulsar } = require('./pulsar');
const { v4: uuidv4 } = require('uuid');
const { dispatchTaskType, taskMessageType } = require('./avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var dt = new dispatchTaskType.getRecordConstructor()
  dt.taskId = uuidv4()
  dt.sentAt = 1588705988
  dt.taskName = "MyTask"
  dt.taskData = { "bytes": Buffer.from('abc') }
  dt.workflowId = { "string": uuidv4() }

  var msg = new taskMessageType.getRecordConstructor()
  msg.type = "DispatchTask"
  msg.taskId = dt.taskId
  msg.DispatchTask = {'com.zenaton.taskmanager.messages.commands.AvroDispatchTask': dt}

  // Send taskDispatched messages
  producer.send({data: taskMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();