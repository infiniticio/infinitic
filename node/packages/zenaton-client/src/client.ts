import {
  ClientOpts as PulsarClientOpts,
  Client as PulsarClient,
  Producer as PulsarProducer,
} from 'pulsar-client';
import {
  AvroForEngineMessage,
  DispatchJobMessage,
  ForEngineMessage,
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

  async dispatchTask(name: string, input: any) {
    const jobId = uuid();
    const message: DispatchJobMessage = {
      jobId,
      type: 'DispatchJob',
      DispatchJob: {
        jobId,
        jobName: name,
        jobData: Buffer.from(JSON.stringify(input)),
        workflowId: null,
      },
    };

    this.dispatchForEngineMessage(message);
  }

  async dispatchForEngineMessage(message: ForEngineMessage) {
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
      data: AvroForEngineMessage.toBuffer(message),
    });
  }
}
