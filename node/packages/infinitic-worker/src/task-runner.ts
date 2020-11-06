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

import { Consumer, Producer } from 'pulsar-client';
import { Task } from './task';
import {
  TaskOutput,
  AvroEnvelopeForWorker,
  RunTask,
  AvroEnvelopeForTaskEngine,
  TaskAttemptStartedMessage,
  TaskAttemptCompletedMessage,
  TaskAttemptFailedMessage,
} from '@infinitic/messages';

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
        case 'RunTask':
          await this.runTask(decodedMessage.RunTask);
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

    let input: any;
    if (message.methodInput.length > 0) {
      input = JSON.parse(message.methodInput[0].bytes.toString());
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

  private async notifyTaskAttemptStarted(message: RunTask) {
    const toSend: TaskAttemptStartedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptStarted',
      TaskAttemptStarted: {
        taskId: message.taskId,
        taskAttemptId: message.taskAttemptId,
        taskAttemptRetry: message.taskAttemptRetry,
        taskRetry: message.taskRetry,
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForTaskEngine.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptCompleted(message: RunTask, output: unknown) {
    let taskOutput: TaskOutput | null;
    if (output === null || output === undefined) {
      taskOutput = null;
    } else {
      taskOutput = {
        bytes: Buffer.from(JSON.stringify(output)),
        type: 'JSON',
        meta: new Map(),
      };
    }

    const toSend: TaskAttemptCompletedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptCompleted',
      TaskAttemptCompleted: {
        taskId: message.taskId,
        taskAttemptId: message.taskAttemptId,
        taskAttemptRetry: message.taskAttemptRetry,
        taskRetry: message.taskRetry,
        taskOutput: taskOutput,
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForTaskEngine.toBuffer(toSend),
    });
  }

  private async notifyTaskAttemptFailed(message: RunTask, error: Error) {
    const toSend: TaskAttemptFailedMessage = {
      taskId: message.taskId,
      type: 'TaskAttemptFailed',
      TaskAttemptFailed: {
        taskId: message.taskId,
        taskAttemptId: message.taskAttemptId,
        taskAttemptRetry: message.taskAttemptRetry,
        taskRetry: message.taskRetry,
        taskAttemptDelayBeforeRetry: null,
        taskAttemptError: {
          bytes: Buffer.from(JSON.stringify(error)),
          type: 'JSON',
          meta: new Map(),
        },
      },
    };

    this.pulsarProducer.send({
      data: AvroEnvelopeForTaskEngine.toBuffer(toSend),
    });
  }
}
