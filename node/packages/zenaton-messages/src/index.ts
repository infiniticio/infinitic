import { typeForSchema, AvroRegistry } from './type';
import path from 'path';

export type TaskEngineMessageType =
  | 'CancelTask'
  | 'DispatchTask'
  | 'RetryTask'
  | 'RetryTaskAttempt'
  | 'TaskAttemptCompleted'
  | 'TaskAttemptDispatched'
  | 'TaskAttemptFailed'
  | 'TaskAttemptStarted'
  | 'TaskCanceled'
  | 'TaskCompleted'
  | 'TaskDispatched';

export type CancelTask = {
  taskId: string;
  sentAt: number;
};

export type DispatchTask = {
  taskId: string;
  sentAt: number;
  taskName: string;
  // TODO: add taskData
  workflowId: string;
};

export type RetryTask = {
  taskId: string;
  sentAt: number;
};

export type RetryTaskAttempt = {
  taskId: string;
  sentAt: number;
  taskAttemptId: string;
  taskAttemptIndex: number;
};

export type TaskAttemptCompleted = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptIndex: number;
  sentAt: number;
  // TODO: add taskOutput
};

export type TaskAttemptDispatched = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptIndex: number;
  sentAt: number;
};

export type TaskAttemptFailed = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptIndex: number;
  sentAt: number;
  // TODO: add taskAttemptError
  taskAttemptDelayBeforeRetry: number | null;
};

export type TaskAttemptStarted = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptIndex: number;
  sentAt: number;
};

export type TaskCanceled = {
  taskId: string;
  sentAt: number;
};

export type TaskCompleted = {
  taskId: string;
  sentAt: number;
  // TODO: add taskOutput
};

export type TaskDispatched = {
  taskId: string;
  sentAt: number;
};

export interface CancelTaskMessage {
  type: 'CancelTask';
  taskId: string;
  CancelTask: CancelTask;
}

export interface DispatchTaskMessage {
  type: 'DispatchTask';
  taskId: string;
  DispatchTask: DispatchTask;
}

export interface RetryTaskMessage {
  type: 'RetryTask';
  taskId: string;
  RetryTask: RetryTask;
}

export interface RetryTaskAttemptMessage {
  type: 'RetryTaskAttempt';
  taskId: string;
  RetryTaskAttempt: RetryTaskAttempt;
}

export interface TaskAttemptCompletedMessage {
  type: 'TaskAttemptCompleted';
  taskId: string;
  TaskAttemptCompleted: TaskAttemptCompleted;
}

export interface TaskAttemptDispatchedMessage {
  type: 'TaskAttemptDispatched';
  taskId: string;
  TaskAttemptDispatched: TaskAttemptDispatched;
}

export interface TaskAttemptFailedMessage {
  type: 'TaskAttemptFailed';
  taskId: string;
  TaskAttemptFailed: TaskAttemptFailed;
}

export interface TaskAttemptStartedMessage {
  type: 'TaskAttemptStarted';
  taskId: string;
  TaskAttemptStarted: TaskAttemptStarted;
}

export interface TaskCanceledMessage {
  type: 'TaskCanceled';
  taskId: string;
  TaskCanceled: TaskCanceled;
}

export interface TaskCompletedMessage {
  type: 'TaskCompleted';
  taskId: string;
  TaskCompleted: TaskCompleted;
}

export interface TaskDispatchedMessage {
  type: 'TaskDispatched';
  taskId: string;
  TaskDispatched: TaskDispatched;
}

export type TaskEngineMessage =
  | CancelTaskMessage
  | DispatchTaskMessage
  | RetryTaskMessage
  | RetryTaskAttemptMessage
  | TaskAttemptCompletedMessage
  | TaskAttemptDispatchedMessage
  | TaskAttemptFailedMessage
  | TaskAttemptStartedMessage
  | TaskCanceledMessage
  | TaskCompletedMessage
  | TaskDispatchedMessage;

export type TaskWorkerMessageType = 'RunTask';

export type RunTask = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptIndex: number;
  sentAt: number;
  taskName: string;
};

export interface RunTaskMessage {
  type: 'RunTask';
  RunTask: RunTask;
}

export type TaskWorkerMessage = RunTaskMessage;

// ------------------------------------------------------------------------------------------------
// Task engine messages definitions
// ------------------------------------------------------------------------------------------------

const registry: AvroRegistry = {};

export const AvroTaskEngineMessageType = typeForSchema<TaskEngineMessageType>(
  path.resolve(
    `${__dirname}/avro/messages/engine/AvroTaskEngineMessageType.avsc`
  ),
  registry
);

export const AvroCancelTask = typeForSchema<CancelTask>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroCancelTask.avsc`),
  registry
);

export const AvroDispatchTask = typeForSchema<DispatchTask>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroDispatchTask.avsc`),
  registry
);

export const AvroRetryTask = typeForSchema<RetryTask>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroRetryTask.avsc`),
  registry
);

export const AvroRetryTaskAttempt = typeForSchema<RetryTaskAttempt>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroRetryTaskAttempt.avsc`),
  registry
);

export const AvroTaskAttemptCompleted = typeForSchema<TaskAttemptCompleted>(
  path.resolve(
    `${__dirname}/avro/messages/engine/AvroTaskAttemptCompleted.avsc`
  ),
  registry
);

export const AvroTaskAttemptDispatched = typeForSchema<TaskAttemptDispatched>(
  path.resolve(
    `${__dirname}/avro/messages/engine/AvroTaskAttemptDispatched.avsc`
  ),
  registry
);

export const AvroTaskAttemptFailed = typeForSchema<TaskAttemptFailed>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskAttemptFailed.avsc`),
  registry
);

export const AvroTaskAttemptStarted = typeForSchema<TaskAttemptStarted>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskAttemptStarted.avsc`),
  registry
);

export const AvroTaskCanceled = typeForSchema<TaskCanceled>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskCanceled.avsc`),
  registry
);

export const AvroTaskCompleted = typeForSchema<TaskCompleted>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskCompleted.avsc`),
  registry
);

export const AvroTaskDispatched = typeForSchema<TaskDispatched>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskDispatched.avsc`),
  registry
);

export const AvroTaskEngineMessage = typeForSchema<TaskEngineMessage>(
  path.resolve(`${__dirname}/avro/messages/engine/AvroTaskEngineMessage.avsc`),
  registry
);

// ------------------------------------------------------------------------------------------------
// Task worker messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroTaskWorkerMessageType = typeForSchema<TaskWorkerMessageType>(
  path.resolve(
    `${__dirname}/avro/messages/workers/AvroTaskWorkerMessageType.avsc`
  ),
  registry
);

export const AvroRunTask = typeForSchema<RunTask>(
  path.resolve(`${__dirname}/avro/messages/workers/AvroRunTask.avsc`),
  registry
);

export const AvroTaskWorkerMessage = typeForSchema<TaskWorkerMessage>(
  path.resolve(`${__dirname}/avro/messages/workers/AvroTaskWorkerMessage.avsc`),
  registry
);
