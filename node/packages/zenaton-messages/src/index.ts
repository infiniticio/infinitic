import { typeForSchema, AvroRegistry } from './type';
import path from 'path';

export interface SerializedData {
  bytes: Buffer;
  type: string;
  meta: Map<string, Buffer>;
}

export type TaskInput = SerializedData[];
export type TaskOutput = SerializedData;
export type TaskAttemptError = SerializedData;
export type TaskOptions = { runningTimeout: number | null };

export type ForTaskEngineMessageType =
  | 'CancelTask'
  | 'DispatchTask'
  | 'TaskAttemptCompleted'
  | 'TaskAttemptDispatched'
  | 'TaskAttemptFailed'
  | 'TaskAttemptStarted'
  | 'TaskCanceled'
  | 'TaskCompleted'
  | 'RetryTask'
  | 'RetryTaskAttempt';

export type CancelTask = {
  taskId: string;
};

export type DispatchTask = {
  taskId: string;
  taskName: string;
  taskInput: TaskInput;
  taskOptions: TaskOptions;
  taskMeta: Map<string, SerializedData>;
};

export type RetryTask = {
  taskId: string;
};

export type RetryTaskAttempt = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
};

export type TaskAttemptCompleted = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
  taskOutput: TaskOutput | null;
};

export type TaskAttemptDispatched = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
};

export type TaskAttemptFailed = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
  taskAttemptError: TaskAttemptError;
  taskAttemptDelayBeforeRetry: number | null;
};

export type TaskAttemptStarted = {
  taskId: string;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
};

export type TaskCanceled = {
  taskId: string;
};

export type TaskCompleted = {
  taskId: string;
  sentAt: number;
  taskOutput: TaskOutput | null;
};

export type TaskCreated = {
  taskId: string;
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
  TaskDispatched: TaskCreated;
}

export type ForTaskEngineMessage =
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

export type ForWorkerMessageType = 'RunTask';

export type RunTask = {
  taskId: string;
  taskName: string;
  taskInput: TaskInput;
  taskAttemptId: string;
  taskAttemptRetry: number;
  taskAttemptIndex: number;
  taskOptions: TaskOptions;
  taskMeta: Map<string, SerializedData>;
};

export interface RunTaskMessage {
  type: 'RunTask';
  RunTask: RunTask;
}

export type ForWorkerMessage = RunTaskMessage;

// ------------------------------------------------------------------------------------------------
// Data definitions
// ------------------------------------------------------------------------------------------------

const registry: AvroRegistry = {};

export const SerializedData = typeForSchema<SerializedData>(
  path.resolve(`${__dirname}/avro/taskManager/data/AvroSerializedData.avsc`),
  registry
);

export const AvroTaskOptions = typeForSchema<CancelTask>(
  path.resolve(`${__dirname}/avro/taskManager/data/AvroTaskOptions.avsc`),
  registry
);

// ------------------------------------------------------------------------------------------------
// Messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroCancelTask = typeForSchema<CancelTask>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroCancelTask.avsc`),
  registry
);

export const AvroDispatchTask = typeForSchema<DispatchTask>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroDispatchTask.avsc`),
  registry
);

export const AvroRetryTask = typeForSchema<RetryTask>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroRetryTask.avsc`),
  registry
);

export const AvroRetryTaskAttempt = typeForSchema<RetryTaskAttempt>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroRetryTaskAttempt.avsc`
  ),
  registry
);

export const AvroTaskAttemptCompleted = typeForSchema<TaskAttemptCompleted>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroTaskAttemptCompleted.avsc`
  ),
  registry
);

export const AvroTaskAttemptDispatched = typeForSchema<TaskAttemptDispatched>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroTaskAttemptDispatched.avsc`
  ),
  registry
);

export const AvroTaskAttemptFailed = typeForSchema<TaskAttemptFailed>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroTaskAttemptFailed.avsc`
  ),
  registry
);

export const AvroTaskAttemptStarted = typeForSchema<TaskAttemptStarted>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroTaskAttemptStarted.avsc`
  ),
  registry
);

export const AvroTaskCanceled = typeForSchema<TaskCanceled>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroTaskCanceled.avsc`),
  registry
);

export const AvroTaskCompleted = typeForSchema<TaskCompleted>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroTaskCompleted.avsc`),
  registry
);

export const AvroTaskCreated = typeForSchema<TaskCreated>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroTaskCreated.avsc`),
  registry
);

export const AvroEnvelopeForTaskEngine = typeForSchema<ForTaskEngineMessage>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/envelopes/AvroEnvelopeForTaskEngine.avsc`
  ),
  registry
);

// ------------------------------------------------------------------------------------------------
// Worker messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroRunTask = typeForSchema<RunTask>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroRunTask.avsc`),
  registry
);

export const AvroEnvelopeForWorker = typeForSchema<ForWorkerMessage>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/envelopes/AvroEnvelopeForWorker.avsc`
  ),
  registry
);
