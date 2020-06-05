import { Consumer, Producer } from 'pulsar-client';
import { Task } from './task';
import {
  AvroTaskWorkerMessage,
  RunTask,
  AvroTaskEngineMessage,
  TaskAttemptStartedMessage,
  TaskAttemptCompletedMessage,
  TaskAttemptFailedMessage,
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
      const decodedMessage = AvroTaskWorkerMessage.fromBuffer(
        message.getData()
      );

      switch (decodedMessage.type) {
        case 'RunTask':
          this.runTask(decodedMessage.RunTask);
          this.pulsarConsumer.acknowledge(message);
          break;
      }
    }
  }

  async stop() {
    this.shouldStop = true;
    await this.pulsarConsumer.close();
  }

  private async runTask(message: RunTask) {
    await this.notifyTaskAttemptStarted(message);

    let output: any = undefined;
    let error: Error | undefined = undefined;
    try {
      output = await this.task.handle();
    } catch (e) {
      error = e;
    }

    if (error) {
      await this.notifyTaskAttemptFailed(message, error);
    } else {
      await this.notifyTaskAttemptCompleted(message, output);
    }
  }

  private async notifyTaskAttemptStarted(message: RunTask) {
    const toSend: TaskAttemptStartedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptStarted',
      TaskAttemptStarted: {
        taskId: message.taskId,
        sentAt: Date.now(),
        taskAttemptId: message.taskAttemptId,
        taskAttemptIndex: message.taskAttemptIndex,
      },
    };

    this.pulsarProducer.send({
      data: AvroTaskEngineMessage.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptCompleted(message: RunTask, _output: unknown) {
    const toSend: TaskAttemptCompletedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptCompleted',
      TaskAttemptCompleted: {
        taskId: message.taskId,
        sentAt: Date.now(),
        taskAttemptId: message.taskAttemptId,
        taskAttemptIndex: message.taskAttemptIndex,
      },
    };

    this.pulsarProducer.send({
      data: AvroTaskEngineMessage.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptFailed(message: RunTask, _error: Error) {
    const toSend: TaskAttemptFailedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptFailed',
      TaskAttemptFailed: {
        taskId: message.taskId,
        sentAt: Date.now(),
        taskAttemptId: message.taskAttemptId,
        taskAttemptIndex: message.taskAttemptIndex,
        taskAttemptDelayBeforeRetry: null,
      },
    };

    this.pulsarProducer.send({
      data: AvroTaskEngineMessage.toBuffer(toSend),
    });
  }
}
