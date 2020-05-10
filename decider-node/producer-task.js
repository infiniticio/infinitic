const { pulsar } = require('./pulsar');
const { taskDispatchedType, taskMessageType } = require('./avro');

(async () => {
  // Create a producer
  const producer = await pulsar.createProducer({
    topic: 'persistent://public/default/tasks',
    sendTimeoutMs: 30000,
    batchingEnabled: false,
  });

  var atd = new taskDispatchedType.getRecordConstructor()
  atd.taskId = "bae25546-1dcb-4206-9fe4-7aaaf526ee08"
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
  await pulsar.close();
})();