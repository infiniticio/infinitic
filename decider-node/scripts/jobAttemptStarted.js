const { pulsar } = require('../pulsar');
const { forEngineMessage } = require('../avro');

const jobId = '6244ce4a-342f-4118-b15a-789467b5a73e';
const jobAttemptId = '6e345496-e39e-41fe-b0e1-f3a54d9aece6';
const jobAttemptIndex = 0;
const jobAttemptRetry = 0;

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
  m.jobAttemptDelayBeforeRetry = 10.0
  m.jobAttemptDelayBeforeTimeout = 3.0

  var msg = new Object()
  msg.type = "JobAttemptStarted"
  msg.jobId = m.jobId
  msg[msg.type] = {'com.zenaton.jobManager.messages.AvroJobAttemptStarted': m}

  // Send message
  producer.send({data: forEngineMessage.toBuffer(msg)});
  await producer.flush();

  await producer.close();
  await pulsar.close();
})();
