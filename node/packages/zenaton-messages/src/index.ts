import { typeForSchema, AvroRegistry } from './type';
import path from 'path';

export type ForEngineMessageType =
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
  sentAt: number;
};

export type DispatchJob = {
  jobId: string;
  sentAt: number;
  jobName: string;
  // TODO: add jobData
  workflowId: string;
};

export type RetryJob = {
  jobId: string;
  sentAt: number;
};

export type RetryJobAttempt = {
  jobId: string;
  sentAt: number;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
};

export type JobAttemptCompleted = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  sentAt: number;
  jobOutput: Buffer | null;
};

export type JobAttemptDispatched = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  sentAt: number;
};

export type JobAttemptFailed = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  sentAt: number;
  jobAttemptError: Buffer;
  jobAttemptDelayBeforeRetry: number | null;
};

export type JobAttemptStarted = {
  jobId: string;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  sentAt: number;
};

export type JobCanceled = {
  jobId: string;
  sentAt: number;
};

export type JobCompleted = {
  jobId: string;
  sentAt: number;
  // TODO: add jobOutput
};

export type JobCreated = {
  jobId: string;
  sentAt: number;
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

export type ForEngineMessage =
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
  jobData: Buffer | null;
  jobAttemptId: string;
  jobAttemptRetry: number;
  jobAttemptIndex: number;
  sentAt: number;
};

export interface RunJobMessage {
  type: 'RunJob';
  RunJob: RunJob;
}

export type ForWorkerMessage = RunJobMessage;

// ------------------------------------------------------------------------------------------------
// Engine messages definitions
// ------------------------------------------------------------------------------------------------

const registry: AvroRegistry = {};

export const AvroCancelJob = typeForSchema<CancelJob>(
  path.resolve(`${__dirname}/avro/messages/AvroCancelJob.avsc`),
  registry
);

export const AvroDispatchJob = typeForSchema<DispatchJob>(
  path.resolve(`${__dirname}/avro/messages/AvroDispatchJob.avsc`),
  registry
);

export const AvroRetryJob = typeForSchema<RetryJob>(
  path.resolve(`${__dirname}/avro/messages/AvroRetryJob.avsc`),
  registry
);

export const AvroRetryJobAttempt = typeForSchema<RetryJobAttempt>(
  path.resolve(`${__dirname}/avro/messages/AvroRetryJobAttempt.avsc`),
  registry
);

export const AvroJobAttemptCompleted = typeForSchema<JobAttemptCompleted>(
  path.resolve(`${__dirname}/avro/messages/AvroJobAttemptCompleted.avsc`),
  registry
);

export const AvroJobAttemptDispatched = typeForSchema<JobAttemptDispatched>(
  path.resolve(`${__dirname}/avro/messages/AvroJobAttemptDispatched.avsc`),
  registry
);

export const AvroJobAttemptFailed = typeForSchema<JobAttemptFailed>(
  path.resolve(`${__dirname}/avro/messages/AvroJobAttemptFailed.avsc`),
  registry
);

export const AvroJobAttemptStarted = typeForSchema<JobAttemptStarted>(
  path.resolve(`${__dirname}/avro/messages/AvroJobAttemptStarted.avsc`),
  registry
);

export const AvroJobCanceled = typeForSchema<JobCanceled>(
  path.resolve(`${__dirname}/avro/messages/AvroJobCanceled.avsc`),
  registry
);

export const AvroJobCompleted = typeForSchema<JobCompleted>(
  path.resolve(`${__dirname}/avro/messages/AvroJobCompleted.avsc`),
  registry
);

export const AvroJobCreated = typeForSchema<JobCreated>(
  path.resolve(`${__dirname}/avro/messages/AvroJobCreated.avsc`),
  registry
);

export const AvroForEngineMessage = typeForSchema<ForEngineMessage>(
  path.resolve(
    `${__dirname}/avro/messages/envelopes/AvroForEngineMessage.avsc`
  ),
  registry
);

// ------------------------------------------------------------------------------------------------
// Worker messages definitions
// ------------------------------------------------------------------------------------------------

export const AvroRunJob = typeForSchema<RunJob>(
  path.resolve(`${__dirname}/avro/messages/AvroRunJob.avsc`),
  registry
);

export const AvroForWorkerMessage = typeForSchema<ForWorkerMessage>(
  path.resolve(
    `${__dirname}/avro/messages/envelopes/AvroForWorkerMessage.avsc`
  ),
  registry
);
