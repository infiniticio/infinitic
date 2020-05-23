const { pulsar } = require('../pulsar');
const { v4: uuidv4 } = require('uuid');
const { taskEngineMessageType } = require('../avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.taskId = uuidv4()
  m.sentAt = 1588705988
  m.taskName = "MyTask"
  m.taskData = { "bytes": Buffer.from('abc') }
  m.workflowId = { "string": uuidv4() }

  var msg = new Object()
  msg.type = "DispatchTask"
  msg.taskId = m.taskId
  msg[msg.type] = {'com.zenaton.taskmanager.messages.engine.AvroDispatchTask': m}

  // Send message
  producer.send({data: taskEngineMessageType.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();