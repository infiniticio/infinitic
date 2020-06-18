import {
  ClientOpts as PulsarClientOpts,
  Client as PulsarClient,
  Producer as PulsarProducer,
} from 'pulsar-client';
import {
  AvroForJobEngineMessage,
  DispatchJobMessage,
  ForJobEngineMessage,
} from '@zenaton/messages';
import { v4 as uuid } from 'uuid';

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

  async dispatchTask(jobName: string, input: any) {
    const jobId = uuid();
    const jobInput =
      input == null
        ? []
        : [
            {
              serializedData: Buffer.from(JSON.stringify(input)),
              serializationType: 'JSON',
            },
          ];
    const message: DispatchJobMessage = {
      jobId,
      type: 'DispatchJob',
      DispatchJob: {
        jobId,
        jobName,
        jobInput,
        workflowId: null,
      },
    };

    this.dispatchForJobEngineMessage(message);
  }

  async dispatchForJobEngineMessage(message: ForJobEngineMessage) {
    if (!this.pulsarClient) {
      this.pulsarClient = new PulsarClient(this.opts.pulsar.client);
    }
    if (!this.pulsarProducer) {
      this.pulsarProducer = await this.pulsarClient.createProducer({
        topic: 'persistent://public/default/tasks-engine',
        sendTimeoutMs: 30000,
        batchingEnabled: false,
      });
    }
    await this.pulsarProducer.send({
      data: AvroForJobEngineMessage.toBuffer(message),
    });
  }
}