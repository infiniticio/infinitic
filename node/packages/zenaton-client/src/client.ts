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
} from '@zenaton/messages';
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
    taskMeta: Map<string, SerializedData> = new Map()
  ) {
    const taskId = uuid();
    const taskInput =
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
        taskInput,
        taskMeta,
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
