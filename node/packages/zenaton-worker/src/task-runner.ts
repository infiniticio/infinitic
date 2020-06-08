import { Consumer, Producer } from 'pulsar-client';
import { Task } from './task';
import {
  AvroForWorkerMessage,
  RunJob,
  AvroForEngineMessage,
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
      const decodedMessage = AvroForWorkerMessage.fromBuffer(message.getData());

      switch (decodedMessage.type) {
        case 'RunJob':
          this.runTask(decodedMessage.RunJob);
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
    if (message.jobData) {
      input = JSON.parse(message.jobData.toString());
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
        sentAt: Date.now(),
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptRetry,
        jobAttemptIndex: message.jobAttemptIndex,
      },
    };

    this.pulsarProducer.send({
      data: AvroForEngineMessage.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptCompleted(message: RunJob, output: unknown) {
    let jobOutput: Buffer | null;
    if (output === null || output === undefined) {
      jobOutput = null;
    } else {
      jobOutput = Buffer.from(JSON.stringify(output));
    }

    const toSend: JobAttemptCompletedMessage = {
      jobId: message.jobId,
      type: 'JobAttemptCompleted',
      JobAttemptCompleted: {
        jobId: message.jobId,
        sentAt: Date.now(),
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptRetry,
        jobAttemptIndex: message.jobAttemptIndex,
        jobOutput: jobOutput,
      },
    };

    this.pulsarProducer.send({
      data: AvroForEngineMessage.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptFailed(message: RunJob, error: Error) {
    const toSend: JobAttemptFailedMessage = {
      jobId: message.jobId,
      type: 'JobAttemptFailed',
      JobAttemptFailed: {
        jobId: message.jobId,
        sentAt: Date.now(),
        jobAttemptId: message.jobAttemptId,
        jobAttemptRetry: message.jobAttemptIndex,
        jobAttemptIndex: message.jobAttemptIndex,
        jobAttemptDelayBeforeRetry: null,
        jobAttemptError: Buffer.from(JSON.stringify(error)),
      },
    };

    this.pulsarProducer.send({
      data: AvroForEngineMessage.toBuffer(toSend),
    });
  }
}
