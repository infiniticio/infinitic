import { Consumer, Producer } from 'pulsar-client';
import { Task } from './task';
import {
  JobOutput,
  AvroEnvelopeForWorker,
  RunJob,
  AvroEnvelopeForJobEngine,
  JobAttemptStartedMessage,
  JobAttemptCompletedMessage,
  JobAttemptFailedMessage,
} from '@zenaton/messages';

export class TaskRunner {
  private shouldStop = false;

  constructor(
    private pulsarConsumer: Consumer,
    private pulsarProducer: Producer,
    private task: Task
  ) {}

  async run(): Promise<void> {
    while (!this.shouldStop) {
      const message = await this.pulsarConsumer.receive();
      const decodedMessage = AvroEnvelopeForWorker.fromBuffer(
        message.getData()
      );

      switch (decodedMessage.type) {
        case 'RunJob':
          await this.runTask(decodedMessage.RunJob);
          this.pulsarConsumer.acknowledge(message);
          break;
      }
    }
  }

  async stop() {
    this.shouldStop = true;
    await this.pulsarConsumer.close();
  }

  private async runTask(message: RunJob) {
    await this.notifyTaskAttemptStarted(message);

    let input: any;
    if (message.jobInput.length > 0) {
      input = JSON.parse(message.jobInput[0].bytes.toString());
    } else {
      input = undefined;
    }
    let output: any = undefined;
    let error: Error | undefined = undefined;
    try {
      output = await this.task.handle(input);
    } catch (e) {
      error = e;
    }

    if (error) {
      await this.notifyTaskAttemptFailed(message, error);
    } else {
      await this.notifyTaskAttemptCompleted(message, output);
    }
  }

  private async notifyTaskAttemptStarted(message: RunJob) {
    const toSend: JobAttemptStartedMessage = {
      jobId: message.jobId,
      type: 'JobAttemptStarted',
      JobAttemptStarted: {
        jobId: message.jobId,
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptRetry,
        jobAttemptIndex: message.jobAttemptIndex,
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForJobEngine.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptCompleted(message: RunJob, output: unknown) {
    let jobOutput: JobOutput | null;
    if (output === null || output === undefined) {
      jobOutput = null;
    } else {
      jobOutput = {
        bytes: Buffer.from(JSON.stringify(output)),
        type: 'JSON',
        meta: new Map(),
      };
    }

    const toSend: JobAttemptCompletedMessage = {
      jobId: message.jobId,
      type: 'JobAttemptCompleted',
      JobAttemptCompleted: {
        jobId: message.jobId,
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptRetry,
        jobAttemptIndex: message.jobAttemptIndex,
        jobOutput: jobOutput,
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForJobEngine.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptFailed(message: RunJob, error: Error) {
    const toSend: JobAttemptFailedMessage = {
      jobId: message.jobId,
      type: 'JobAttemptFailed',
      JobAttemptFailed: {
        jobId: message.jobId,
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptRetry,
        jobAttemptIndex: message.jobAttemptIndex,
        jobAttemptDelayBeforeRetry: null,
        jobAttemptError: {
          bytes: Buffer.from(JSON.stringify(error)),
          type: 'JSON',
          meta: new Map()
        },
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForJobEngine.toBuffer(toSend),
    });
  }
}
