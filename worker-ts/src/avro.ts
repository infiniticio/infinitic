import { Type } from 'avsc';
import { readFileSync } from 'fs';

type AvroRegistry = { [name: string]: Type };

const registry: AvroRegistry = {};

function loadDefinition(file: string, registry: AvroRegistry) {
  const fileContent = readFileSync(file);
  const parsedContent = JSON.parse(fileContent.toString());

  return Type.forSchema(parsedContent, {
    registry: registry,
  });
}

// Task engine messages
export const AvroTaskEngineMessageType = loadDefinition(
  __dirname +
    '/../resources/avro/taskmanager/messages/engine/AvroTaskEngineMessageType.avsc',
  registry
);
export const AvroCancelTask = loadDefinition(
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
