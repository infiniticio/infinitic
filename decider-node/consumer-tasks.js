const Pulsar = require('pulsar-client');
const avro = require('avro-js');

var registry = {}
var taskAttemptCompletedType = avro.parse("./avro/tasks/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptFailedType = avro.parse("./avro/tasks/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptRetriedType = avro.parse("./avro/tasks/AvroTaskAttemptRetried.avsc", { registry });
var taskAttemptStartedType = avro.parse("./avro/tasks/AvroTaskAttemptStarted.avsc", { registry });
var taskAttemptTimeoutType = avro.parse("./avro/tasks/AvroTaskAttemptTimeout.avsc", { registry });
var taskDispatchedType = avro.parse("./avro/tasks/AvroTaskDispatched.avsc", { registry });
var taskMessageType = avro.parse("./avro/tasks/AvroTaskMessage.avsc", { registry });


(async () => {
  // Create a client
  const client = new Pulsar.Client({
    serviceUrl: 'pulsar://localhost:6650',
    operationTimeoutSeconds: 30,
  });

  // Create a consumer
  const consumer = await client.subscribe({
    topic: 'persistent://public/default/tasks',
    subscription: 'subTasks',
    subscriptionType: 'Exclusive',
    ackTimeoutMs: 10000,
  });

  // Receive messages
  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    console.log(Object.values(taskMessageType.fromBuffer(msg.getData()).msg)[0])
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await client.close();
})();