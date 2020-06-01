import { Type as AvscType, Resolver } from 'avsc';
import { readFileSync } from 'fs';

type AvroRegistry = { [name: string]: Type<TaskEngineMessage> };

const registry: AvroRegistry = {};

function loadDefinition(
  file: string,
  registry: AvroRegistry
): Type<TaskEngineMessage> {
  const fileContent = readFileSync(file);
  const parsedContent = JSON.parse(fileContent.toString());

  return AvscType.forSchema(parsedContent, {
    registry: registry,
  }) as Type<TaskEngineMessage>;
}

export interface DispatchTaskMessage {
  type: 'DispatchTask';
  taskId: string;
  DispatchTask: {
    taskId: string;
    sentAt: number;
    taskName: string;
    workflowId: string;
  };
}

export interface TaskDispatched {
  type: 'TaskDispatched';
  taskId: string;
  TaskDispatched: {
    taskId: string;
    sentAt: number;
  };
}

export interface TaskAttemptDispatched {
  type: 'TaskAttemptDispatched';
  taskId: string;
  TaskAttemptDispatched: {
    taskId: string;
    taskAttemptId: string;
    taskAttemptIndex: number;
    sentAt: number;
  };
}

export type TaskEngineMessage =
  | DispatchTaskMessage
  | TaskDispatched
  | TaskAttemptDispatched;

export interface Type<T> extends AvscType {
  fromBuffer(buffer: Buffer, resolver?: Resolver, noCheck?: boolean): T;
}

// Task engine messages definitions
export const AvroTaskEngineMessageTypeSchema = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskEngineMessageType.avsc',
  registry
);

export const AvroCancelTaskDefinition = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroCancelTask.avsc',
  registry
);

export const AvroDispatchTask = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroDispatchTask.avsc',
  registry
);

export const AvroRetryTask = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroRetryTask.avsc',
  registry
);

export const AvroRetryTaskAttempt = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroRetryTaskAttempt.avsc',
  registry
);

export const AvroTaskAttemptCompleted = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskAttemptCompleted.avsc',
  registry
);

export const AvroTaskAttemptDispatched = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskAttemptDispatched.avsc',
  registry
);

export const AvroTaskAttemptFailed = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskAttemptFailed.avsc',
  registry
);

export const AvroTaskAttemptStarted = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskAttemptStarted.avsc',
  registry
);

export const AvroTaskCanceled = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskCanceled.avsc',
  registry
);

export const AvroTaskCompleted = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskCompleted.avsc',
  registry
);

export const AvroTaskDispatched = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskDispatched.avsc',
  registry
);

export const AvroTaskEngineMessage = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskEngineMessage.avsc',
  registry
);

export const AvroTaskWorkerMessageType = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/workers/AvroTaskworkerMessageType.avsc',
  registry
);

export const AvroRunTask = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/workers/AvroRunTask.avsc',
  registry
);

export const AvroTaskWorkerMessage = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/workers/AvroTaskWorkerMessage.avsc',
  registry
);
