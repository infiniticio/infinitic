const { pulsar } = require('../pulsar');
const { v4: uuidv4 } = require('uuid');
const { forEngineMessage } = require('../avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/engine',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.jobId = uuidv4()
  m.sentAt = 1588705988
  m.jobName = "MyTask3"
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