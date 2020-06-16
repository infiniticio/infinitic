const { pulsar } = require('../pulsar');
const { v4: uuidv4 } = require('uuid');
const { forEngineMessage } = require('../avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks-engine',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.jobId = uuidv4()
  m.jobName = "MyTask18"
  m.jobData = { "bytes": Buffer.from('abc') }
  m.workflowId = { "string": uuidv4() }

  var msg = new Object()
  msg.type = "DispatchJob"
  msg.jobId = m.jobId
  msg[msg.type] = {'com.zenaton.jobManager.messages.AvroDispatchJob': m}

  // Send message
  producer.send({data: forEngineMessage.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();
