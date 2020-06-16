const { pulsar } = require('../pulsar');
const { forEngineMessage } = require('../avro');

const jobId = '15a243f6-5477-4dd7-9ebe-c89d8ad6744c';

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/engine',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.jobId = jobId

  var msg = new Object()
  msg.type = "Retryjob"
  msg.jobId = m.jobId
  msg[msg.type] = {"com.zenaton.jobManager.messages.AvroRetryJob": m}

  // Send message
  producer.send({data: forEngineMessage.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();
