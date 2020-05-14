const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});
  
    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

var dispatchTaskType = avro.parse("./avro/taskmanager/messages/commands/AvroDispatchTask.avsc", { registry });
var retryTaskType = avro.parse("./avro/taskmanager/messages/commands/AvroRetryTask.avsc", { registry });
var retryTaskAttemptType = avro.parse("./avro/taskmanager/messages/commands/AvroRetryTaskAttempt.avsc", { registry });
var timeOutTaskAttemptType = avro.parse("./avro/taskmanager/messages/commands/AvroTimeOutTaskAttempt.avsc", { registry });

var taskAttemptCompletedType = avro.parse("./avro/taskmanager/messages/events/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptDispatchedType = avro.parse("./avro/taskmanager/messages/events/AvroTaskAttemptDispatched.avsc", { registry });
var taskAttemptFailedType = avro.parse("./avro/taskmanager/messages/events/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptStartedType = avro.parse("./avro/taskmanager/messages/events/AvroTaskAttemptStarted.avsc", { registry });
var taskAttemptTimedOutType = avro.parse("./avro/taskmanager/messages/events/AvroTaskAttemptTimedOut.avsc", { registry });

var taskMessageType = avro.parse("./avro/taskmanager/messages/AvroTaskMessage.avsc", { registry  });
var runTaskType = avro.parse("./avro/taskmanager/messages/AvroRunTask.avsc", { registry  });


module.exports = {
    assertValid,
    dispatchTaskType,
    retryTaskType,
    retryTaskAttemptType,
    timeOutTaskAttemptType,
    taskAttemptCompletedType,
    taskAttemptDispatchedType,
    taskAttemptFailedType,
    taskAttemptStartedType,
    taskAttemptTimedOutType,
    taskMessageType,
    runTaskType
}
