import * as types from '../src';
import { Type } from '../src/type';

function random<T>(type: Type<T>): T {
  const value = type.random();

  return (value as unknown) as T;
}

describe('@zenaton/messages', () => {
  it('exports a correct AvroCancelJob', () => {
    const value = random(types.AvroCancelJob);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
  });

  it('exports a correct AvroDispatchJob', () => {
    const value = random(types.AvroDispatchJob);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobName).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
    expect(value.workflowId).toBeOfTypeOrNull('string');
  });

  it('exports a correct AvroJobAttemptCompleted', () => {
    const value = random(types.AvroJobAttemptCompleted);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.sentAt).toBeOfType('number');
  });

  it('exports a correct AvroJobAttemptDispatched', () => {
    const value = random(types.AvroJobAttemptDispatched);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptRetry).toBeOfType('number');
  });

  it('exports a correct AvroJobAttemptFailed', () => {
    const value = random(types.AvroJobAttemptFailed);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptDelayBeforeRetry).toBeOfTypeOrNull('number');
  });

  it('exports a correct AvroJobAttemptStarted', () => {
    const value = random(types.AvroJobAttemptStarted);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
  });

  it('exports a correct AvroJobCanceled', () => {
    const value = random(types.AvroJobCanceled);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
  });

  it('exports a correct AvroJobCompleted', () => {
    const value = random(types.AvroJobCompleted);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
  });

  it('exports a correct AvroRetryJob', () => {
    const value = random(types.AvroRetryJob);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
  });

  it('exports a correct AvroRetryJobAttempt', () => {
    const value = random(types.AvroRetryJobAttempt);
    expect(value.jobId).toBeOfType('string');
    expect(value.sentAt).toBeOfType('number');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
  });
});
