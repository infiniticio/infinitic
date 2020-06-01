import Pulsar from 'pulsar-client';
import { AvroTaskEngineMessage, AvroTaskWorkerMessage } from './avro';

export async function runTaskConsumer() {
  const client = new Pulsar.Client({
    serviceUrl: 'pulsar://localhost:6650',
    operationTimeoutSeconds: 30,
  });

  const consumer = await client.subscribe({
    topic: 'persistent://public/default/tasks',
    subscription: 'subTasks',
    subscriptionType: 'Shared',
    ackTimeoutMs: 10000,
  });

  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    const decodedMessage = AvroTaskEngineMessage.fromBuffer(msg.getData());
    switch (decodedMessage.type) {
      case 'DispatchTask':
        console.log(`
        TASK DISPATCH
        id=${decodedMessage.DispatchTask.taskId}
        name=${decodedMessage.DispatchTask.taskName}

        `);
        break;

      case 'TaskAttemptDispatched':
        console.log(`
        TASK DISPATCH
        task-id=${decodedMessage.TaskAttemptDispatched.taskId}
        id=${decodedMessage.TaskAttemptDispatched.taskAttemptId}
        index=${decodedMessage.TaskAttemptDispatched.taskAttemptIndex}

        `);
        break;

      default:
        console.log(decodedMessage);
        break;
    }

    consumer.acknowledge(msg);
  }

  await consumer.close();
  await client.close();
}

export async function runTaskAttemptsConsumer(taskname: string) {
  const client = new Pulsar.Client({
    serviceUrl: 'pulsar://localhost:6650',
    operationTimeoutSeconds: 30,
  });

  const consumer = await client.subscribe({
    topic: `persistent://public/default/tasks-${taskname}`,
    subscription: `subTaskAttempts-${taskname}`,
    subscriptionType: 'Shared',
    ackTimeoutMs: 10000,
  });

  for (let i = 0; i < 1000; i += 1) {
    const msg = await consumer.receive();
    console.log(AvroTaskWorkerMessage.fromBuffer(msg.getData()));
    consumer.acknowledge(msg);
  }

  await consumer.close();
  await client.close();
}
