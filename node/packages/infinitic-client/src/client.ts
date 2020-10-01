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

import {
  ClientOpts as PulsarClientOpts,
  Client as PulsarClient,
  Producer as PulsarProducer,
} from 'pulsar-client';
import {
  AvroEnvelopeForTaskEngine,
  DispatchTaskMessage,
  ForTaskEngineMessage,
  SerializedData,
  TaskOptions,
} from '@infinitic/messages';
import { v4 as uuid } from 'uuid';
import pLimit from 'p-limit';

const lock = pLimit(1);

export interface ClientOpts {
  pulsar: {
    client: PulsarClientOpts;
  };
}

export class Client {
  private opts: ClientOpts;
  private pulsarClient?: PulsarClient;
  private pulsarProducer?: PulsarProducer;

  constructor(opts: Partial<ClientOpts> = {}) {
    this.opts = Object.assign(
      {
        pulsar: {
          client: {},
        },
      },
      opts
    );
  }

  async dispatchTask(
    taskName: string,
    input: any = null,
    taskMeta: Map<string, SerializedData> = new Map(),
    taskOptions: TaskOptions = { runningTimeout: null }
  ) {
    const taskId = uuid();
    const methodInput =
      input == null
        ? []
        : [
            {
              bytes: Buffer.from(JSON.stringify(input)),
              type: 'JSON',
              meta: new Map(),
            },
          ];
    const message: DispatchTaskMessage = {
      taskId,
      type: 'DispatchTask',
      DispatchTask: {
        taskId,
        taskName,
        methodName: 'handle',
        methodInput,
        taskMeta,
        taskOptions,
      },
    };

    this.dispatchForTaskEngineMessage(message);
  }

  async close() {
    if (this.pulsarProducer) {
      await this.pulsarProducer.close();
    }
    if (this.pulsarClient) {
      await this.pulsarClient.close();
    }
  }

  private async dispatchForTaskEngineMessage(message: ForTaskEngineMessage) {
    await this.initializeClientAndProducer();

    await this.pulsarProducer!.send({
      data: AvroEnvelopeForTaskEngine.toBuffer(message),
    });
  }

  /**
   * This method will lazily initialize the pulsarClient and pulsarProducer properties
   * while protecting against concurrent initialization.
   */
  private initializeClientAndProducer() {
    return lock(async () => {
      if (!this.pulsarClient) {
        this.pulsarClient = new PulsarClient(this.opts.pulsar.client);
      }

      if (!this.pulsarProducer) {
        this.pulsarProducer = await this.pulsarClient!.createProducer({
          topic: 'persistent://public/default/tasks-engine',
          sendTimeoutMs: 30000,
          batchingEnabled: false,
        });
      }
    });
  }
}
