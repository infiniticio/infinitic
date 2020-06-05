import { ClientOpts, Client, Producer } from 'pulsar-client';
import { Task } from './task';
import { TaskRunner } from './task-runner';

export interface WorkerOpts {
  pulsar: {
    client: ClientOpts;
  };
}

type TaskRegistry = Map<Task['name'], Task>;

type RunnerRegistry = TaskRunner[];

export class Worker {
  private taskRegistry: TaskRegistry = new Map();
  private runnerRegistry: RunnerRegistry = [];
  private opts: WorkerOpts;
  private pulsarClient?: Client;
  private pulsarProducer?: Producer;

  constructor(opts: Partial<WorkerOpts>) {
    this.opts = Object.assign(
      {},
      {
        pulsar: {
          client: {},
        },
      },
      opts
    );
  }

  registerTask(task: Task) {
    if (this.taskRegistry.has(task.name)) {
      throw new Error('Cannot register two tasks with the same name.');
    }

    this.taskRegistry.set(task.name, task);
  }

  async run() {
    if (!this.pulsarClient) {
      this.pulsarClient = new Client(this.opts.pulsar.client);
    }
    if (!this.pulsarProducer) {
      this.pulsarProducer = await this.pulsarClient.createProducer({
        topic: 'persistent://public/default/tasks',
        sendTimeoutMs: 30000,
        batchingEnabled: false,
      });
    }

    for (const [, task] of this.taskRegistry) {
      const consumer = await this.pulsarClient.subscribe({
        topic: `persistent://public/default/tasks-${task.name}`,
        subscription: `subTaskAttempts-${task.name}`,
        subscriptionType: 'Shared',
        ackTimeoutMs: 10000,
      });

      const taskRunner = new TaskRunner(consumer, this.pulsarProducer, task);
      this.runnerRegistry.push(taskRunner);
      taskRunner.run();
    }
  }
}
