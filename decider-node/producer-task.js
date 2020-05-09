const Pulsar = require('pulsar-client');
const util = require('util');
const avro = require('avro-js');

function assertValid(type, val) {
  return type.isValid(val, {errorHook: hook});

  function hook(path, any) {
    throw new Error(util.format('invalid %s: %j', path.join(), any));
  }
}

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

  var registry = {}
  var taskAttemptCompletedType = avro.parse("./avro/tasks/AvroTaskAttemptCompleted.avsc", { registry });
  var taskAttemptFailedType = avro.parse("./avro/tasks/AvroTaskAttemptFailed.avsc", { registry });
  var taskAttemptRetriedType = avro.parse("./avro/tasks/AvroTaskAttemptRetried.avsc", { registry });
  var taskAttemptStartedType = avro.parse("./avro/tasks/AvroTaskAttemptStarted.avsc", { registry });
  var taskAttemptTimeoutType = avro.parse("./avro/tasks/AvroTaskAttemptTimeout.avsc", { registry });
  var taskDispatchedType = avro.parse("./avro/tasks/AvroTaskDispatched.avsc", { registry });
  var taskMessageType = avro.parse("./avro/tasks/AvroTaskMessage.avsc", { registry });

  var atd = new taskDispatchedType.getRecordConstructor()
  atd.taskId = "bae25546-1dcb-4206-9fe4-7aaaf526ee07"
  atd.sentAt = 1588705988
  atd.taskName = "myTask"
  atd.taskData = { "bytes": Buffer.from('abc') }
  atd.workflowId = { "string": 'OqUUovQGsMMAbsdfYBUPsQ' }

  var atm = new taskMessageType.getRecordConstructor()
  atm.type = "TaskDispatched"
  atm.msg = {'com.zenaton.messages.topics.tasks.AvroTaskDispatched': atd}

  var buf = taskMessageType.toBuffer(atm); // Serialized object.

  // Send messages
  producer.send({data: buf});
  await producer.flush();

  await producer.close();
  await client.close();
})();