const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});
  
    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

var taskAttemptCompletedType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptFailedType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptMessageType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptMessage.avsc", { registry });
var taskAttemptRetriedType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptRetried.avsc", { registry });
var taskAttemptStartedType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptStarted.avsc", { registry });
var taskAttemptTimeoutType = avro.parse("./avro/taskmanager/messages/AvroTaskAttemptTimeout.avsc", { registry });
var taskDispatchedType = avro.parse("./avro/taskmanager/messages/AvroTaskDispatched.avsc", { registry });
var taskMessageType = avro.parse("./avro/taskmanager/messages/AvroTaskMessage.avsc", { registry  });


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
