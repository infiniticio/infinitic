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
  });

  it('exports a correct AvroDispatchJob', () => {
    const value = random(types.AvroDispatchJob);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobName).toBeOfType('string');
    // expect(value.jobMeta).toBeOfType('Map');
  });

  it('exports a correct AvroTaskAttemptCompleted', () => {
    const value = random(types.AvroTaskAttemptCompleted);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptRetry).toBeOfType('number');
  });

  it('exports a correct AvroTaskAttemptDispatched', () => {
    const value = random(types.AvroTaskAttemptDispatched);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptRetry).toBeOfType('number');
  });

  it('exports a correct AvroTaskAttemptFailed', () => {
    const value = random(types.AvroTaskAttemptFailed);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
    expect(value.jobAttemptDelayBeforeRetry).toBeOfTypeOrNull('number');
  });

  it('exports a correct AvroTaskAttemptStarted', () => {
    const value = random(types.AvroTaskAttemptStarted);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
  });

  it('exports a correct AvroTaskCanceled', () => {
    const value = random(types.AvroTaskCanceled);
    expect(value.jobId).toBeOfType('string');
  });

  it('exports a correct AvroTaskCompleted', () => {
    const value = random(types.AvroTaskCompleted);
    expect(value.jobId).toBeOfType('string');
  });

  it('exports a correct AvroRetryJob', () => {
    const value = random(types.AvroRetryJob);
    expect(value.jobId).toBeOfType('string');
  });

  it('exports a correct AvroRetryJobAttempt', () => {
    const value = random(types.AvroRetryJobAttempt);
    expect(value.jobId).toBeOfType('string');
    expect(value.jobAttemptId).toBeOfType('string');
    expect(value.jobAttemptRetry).toBeOfType('number');
    expect(value.jobAttemptIndex).toBeOfType('number');
  });
});
