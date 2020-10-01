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

import * as types from '../src';
import { Type } from '../src/type';

function random<T>(type: Type<T>): T {
  const value = type.random();

  return (value as unknown) as T;
}

describe('@infinitic/messages', () => {
  it('exports a correct AvroCancelTask', () => {
    const value = random(types.AvroCancelTask);
    expect(value.taskId).toBeOfType('string');
  });

  it('exports a correct AvroDispatchTask', () => {
    const value = random(types.AvroDispatchTask);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskName).toBeOfType('string');
    expect(value.methodName).toBeOfType('string');
    // expect(value.taskMeta).toBeOfType('Map');
  });

  it('exports a correct AvroTaskAttemptCompleted', () => {
    const value = random(types.AvroTaskAttemptCompleted);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskAttemptId).toBeOfType('string');
    expect(value.taskAttemptIndex).toBeOfType('number');
    expect(value.taskAttemptRetry).toBeOfType('number');
  });

  it('exports a correct AvroTaskAttemptDispatched', () => {
    const value = random(types.AvroTaskAttemptDispatched);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskAttemptId).toBeOfType('string');
    expect(value.taskAttemptIndex).toBeOfType('number');
    expect(value.taskAttemptRetry).toBeOfType('number');
  });

  it('exports a correct AvroTaskAttemptFailed', () => {
    const value = random(types.AvroTaskAttemptFailed);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskAttemptId).toBeOfType('string');
    expect(value.taskAttemptRetry).toBeOfType('number');
    expect(value.taskAttemptIndex).toBeOfType('number');
    expect(value.taskAttemptDelayBeforeRetry).toBeOfTypeOrNull('number');
  });

  it('exports a correct AvroTaskAttemptStarted', () => {
    const value = random(types.AvroTaskAttemptStarted);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskAttemptId).toBeOfType('string');
    expect(value.taskAttemptRetry).toBeOfType('number');
    expect(value.taskAttemptIndex).toBeOfType('number');
  });

  it('exports a correct AvroTaskCanceled', () => {
    const value = random(types.AvroTaskCanceled);
    expect(value.taskId).toBeOfType('string');
  });

  it('exports a correct AvroTaskCompleted', () => {
    const value = random(types.AvroTaskCompleted);
    expect(value.taskId).toBeOfType('string');
  });

  it('exports a correct AvroRetryTask', () => {
    const value = random(types.AvroRetryTask);
    expect(value.taskId).toBeOfType('string');
  });

  it('exports a correct AvroRetryTaskAttempt', () => {
    const value = random(types.AvroRetryTaskAttempt);
    expect(value.taskId).toBeOfType('string');
    expect(value.taskAttemptId).toBeOfType('string');
    expect(value.taskAttemptRetry).toBeOfType('number');
    expect(value.taskAttemptIndex).toBeOfType('number');
  });
});
