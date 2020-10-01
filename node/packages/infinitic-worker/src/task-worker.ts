/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

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
        topic: 'persistent://public/default/tasks-engine',
        sendTimeoutMs: 30000,
        batchingEnabled: false,
      });
    }

    for (const [, task] of this.taskRegistry) {
      const consumer = await this.pulsarClient.subscribe({
        topic: `persistent://public/default/tasks-workers-${task.name}`,
        subscription: `infinitic-worker`,
        subscriptionType: 'Shared',
        ackTimeoutMs: 10000,
      });

      const taskRunner = new TaskRunner(consumer, this.pulsarProducer, task);
      this.runnerRegistry.push(taskRunner);
      taskRunner.run();
    }
  }
}
