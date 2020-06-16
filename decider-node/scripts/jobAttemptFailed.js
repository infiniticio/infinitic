const { pulsar } = require('../pulsar');
const { forEngineMessage } = require('../avro');

const jobId = '563b520d-f0b1-458f-a49f-4d8e8274885f';
const jobAttemptId = '967fee3c-36ad-42f3-9cc9-7de72343e9b5';
const jobAttemptIndex = 0;
const jobAttemptRetry = 1;

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/engine',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var m = new Object()
  m.jobId = jobId
  m.jobAttemptId = jobAttemptId
  m.jobAttemptIndex = jobAttemptIndex
  m.jobAttemptRetry = jobAttemptRetry
  m.jobAttemptError = Buffer.from('Error!')
  m.jobAttemptDelayBeforeRetry = 5.0

  var msg = new Object()
  msg.type = "JobAttemptFailed"
  msg.jobId = m.jobId
  msg[msg.type] = {'com.zenaton.jobManager.messages.AvroJobAttemptFailed': m}

  // Send message
  producer.send({data: forEngineMessage.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();
