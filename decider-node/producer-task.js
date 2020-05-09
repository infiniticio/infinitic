const Pulsar = require('pulsar-client');

(async () => {
  // Create a client
  const client = new Pulsar.Client({
    serviceUrl: 'pulsar://localhost:6650',
    operationTimeoutSeconds: 30,
  });

  // Create a producer
  const producer = await client.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: true,
  });

  var util = require('util');
  var avro = require('avro-js');
  var type = avro.parse("./tasks.asvc");

  class AvroTaskDispatched {}
  class AvroTaskMessage {}

  var atd = new AvroTaskDispatched()
  atd.taskId = "bae25546-1dcb-4206-9fe4-7aaaf526ee07"
  atd.sentAt = 1588705988
  atd.taskName = "myTask"
  atd.taskData = { "bytes": Buffer.from('abc') }
  atd.workflowId = { "string": 'OqUUovQGsMMAbsdfYBUPsQ' }

  var avro = new AvroTaskMessage()
  avro.type = "TaskDispatched"
  avro.msg = {'com.zenaton.messages.topics.tasks.AvroTaskDispatched': atd}

function assertValid(type, val) {
  return type.isValid(val, {errorHook: hook});

  function hook(path, any) {
    throw new Error(util.format('invalid %s: %j', path.join(), any));
  }
}

try {
  assertValid(type, avro); // Will throw.
} catch (err) {
  console.log(err)
  // err.message === 'invalid age: null'
}

  var buf = type.toBuffer(avro); // Serialized object.

  // Send messages
  producer.send({data: buf});

  await producer.flush();

  await producer.close();
  await client.close();
})();