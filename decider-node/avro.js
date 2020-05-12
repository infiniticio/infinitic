const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});
  
    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

var taskAttemptCompletedType = avro.parse("./avro/tasks/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptFailedType = avro.parse("./avro/tasks/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptRetriedType = avro.parse("./avro/tasks/AvroTaskAttemptRetried.avsc", { registry });
var taskAttemptStartedType = avro.parse("./avro/tasks/AvroTaskAttemptStarted.avsc", { registry });
var taskAttemptTimeoutType = avro.parse("./avro/tasks/AvroTaskAttemptTimeout.avsc", { registry });
var taskDispatchedType = avro.parse("./avro/tasks/AvroTaskDispatched.avsc", { registry });
var taskMessageType = avro.parse("./avro/tasks/AvroTaskMessage.avsc", { registry });

var taskAttemptMessageType = avro.parse("./avro/taskAttempts/AvroTaskAttemptMessage.avsc", { registry });

module.exports = {
    assertValid,
    taskAttemptCompletedType,
    taskAttemptFailedType,
    taskAttemptRetriedType,
    taskAttemptStartedType,
    taskAttemptTimeoutType,
    taskDispatchedType,
    taskMessageType,
    taskAttemptMessageType
}
