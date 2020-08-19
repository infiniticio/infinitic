import { typeForSchema, AvroRegistry } from './type';
import path from 'path';

export interface SerializedData {
  bytes: Buffer;
  type: string;
  meta: Map<string, Buffer>;
}

export type JobInput = SerializedData[];
export type JobOutput = SerializedData;
export type JobAttemptError = SerializedData;

export type ForJobEngineMessageType =
  | 'CancelJob'
  | 'DispatchJob'
  | 'JobAttemptCompleted'
  | 'JobAttemptDispatched'
  | 'JobAttemptFailed'
  | 'JobAttemptStarted'
  | 'JobCanceled'
  | 'JobCompleted'
  | 'RetryJob'
  | 'RetryJobAttempt';

export type CancelJob = {
  jobId: string;
};

export type DispatchJob = {
  jobId: string;
  jobName: string;
  jobInput: JobInput;
  jobMeta: Map<string, SerializedData>;
};

export type RetryJob = {
  jobId: string;
};

export type RetryJobAttempt = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
};

export type JobAttemptCompleted = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  jobOutput: JobOutput | null;
};

export type JobAttemptDispatched = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
};

export type JobAttemptFailed = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  jobAttemptError: JobAttemptError;
  jobAttemptDelayBeforeRetry: number | null;
};

export type JobAttemptStarted = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
};

export type JobCanceled = {
  jobId: string;
};

export type JobCompleted = {
  jobId: string;
  sentAt: number;
  jobOutput: JobOutput | null;
};

export type JobCreated = {
  jobId: string;
};

export interface CancelJobMessage {
  type: 'CancelJob';
  jobId: string;
  CancelJob: CancelJob;
}

export interface DispatchJobMessage {
  type: 'DispatchJob';
  jobId: string;
  DispatchJob: DispatchJob;
}

export interface RetryJobMessage {
  type: 'RetryJob';
  jobId: string;
  RetryJob: RetryJob;
}

export interface RetryJobAttemptMessage {
  type: 'RetryJobAttempt';
  jobId: string;
  RetryJobAttempt: RetryJobAttempt;
}

export interface JobAttemptCompletedMessage {
  type: 'JobAttemptCompleted';
  jobId: string;
  JobAttemptCompleted: JobAttemptCompleted;
}

export interface JobAttemptDispatchedMessage {
  type: 'JobAttemptDispatched';
  jobId: string;
  JobAttemptDispatched: JobAttemptDispatched;
}

export interface JobAttemptFailedMessage {
  type: 'JobAttemptFailed';
  jobId: string;
  JobAttemptFailed: JobAttemptFailed;
}

export interface JobAttemptStartedMessage {
  type: 'JobAttemptStarted';
  jobId: string;
  JobAttemptStarted: JobAttemptStarted;
}

export interface JobCanceledMessage {
  type: 'JobCanceled';
  jobId: string;
  JobCanceled: JobCanceled;
}

export interface JobCompletedMessage {
  type: 'JobCompleted';
  jobId: string;
  JobCompleted: JobCompleted;
}

export interface JobDispatchedMessage {
  type: 'JobDispatched';
  jobId: string;
  JobDispatched: JobCreated;
}

export type ForJobEngineMessage =
  | CancelJobMessage
  | DispatchJobMessage
  | RetryJobMessage
  | RetryJobAttemptMessage
  | JobAttemptCompletedMessage
  | JobAttemptDispatchedMessage
  | JobAttemptFailedMessage
  | JobAttemptStartedMessage
  | JobCanceledMessage
  | JobCompletedMessage
  | JobDispatchedMessage;

export type ForWorkerMessageType = 'RunJob';

export type RunJob = {
  jobId: string;
  jobName: string;
  jobInput: JobInput;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
};

export interface RunJobMessage {
  type: 'RunJob';
  RunJob: RunJob;
}

export type ForWorkerMessage = RunJobMessage;

// ------------------------------------------------------------------------------------------------
// Common definitions
// ------------------------------------------------------------------------------------------------

const registry: AvroRegistry = {};

export const SerializedData = typeForSchema<SerializedData>(
  path.resolve(`${__dirname}/avro/taskManager/data/AvroSerializedData.avsc`),
  registry
);

// ------------------------------------------------------------------------------------------------
// Job Engine messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroCancelJob = typeForSchema<CancelJob>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroCancelJob.avsc`),
  registry
);

export const AvroDispatchJob = typeForSchema<DispatchJob>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroDispatchJob.avsc`),
  registry
);

export const AvroRetryJob = typeForSchema<RetryJob>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroRetryJob.avsc`),
  registry
);

export const AvroRetryJobAttempt = typeForSchema<RetryJobAttempt>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroRetryJobAttempt.avsc`
  ),
  registry
);

export const AvroJobAttemptCompleted = typeForSchema<JobAttemptCompleted>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroJobAttemptCompleted.avsc`
  ),
  registry
);

export const AvroJobAttemptDispatched = typeForSchema<JobAttemptDispatched>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroJobAttemptDispatched.avsc`
  ),
  registry
);

export const AvroJobAttemptFailed = typeForSchema<JobAttemptFailed>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroJobAttemptFailed.avsc`
  ),
  registry
);

export const AvroJobAttemptStarted = typeForSchema<JobAttemptStarted>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/AvroJobAttemptStarted.avsc`
  ),
  registry
);

export const AvroJobCanceled = typeForSchema<JobCanceled>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroJobCanceled.avsc`),
  registry
);

export const AvroJobCompleted = typeForSchema<JobCompleted>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroJobCompleted.avsc`),
  registry
);

export const AvroJobCreated = typeForSchema<JobCreated>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroJobCreated.avsc`),
  registry
);

export const AvroEnvelopeForJobEngine = typeForSchema<ForJobEngineMessage>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/envelopes/AvroEnvelopeForJobEngine.avsc`
  ),
  registry
);

// ------------------------------------------------------------------------------------------------
// Worker messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroRunJob = typeForSchema<RunJob>(
  path.resolve(`${__dirname}/avro/taskManager/messages/AvroRunJob.avsc`),
  registry
);

export const AvroEnvelopeForWorker = typeForSchema<ForWorkerMessage>(
  path.resolve(
    `${__dirname}/avro/taskManager/messages/envelopes/AvroEnvelopeForWorker.avsc`
  ),
  registry
);
