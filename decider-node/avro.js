const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});
  
    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

var cancelTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/commands/AvroCancelTask.avsc", { registry });
var dispatchTaskType = avro.parse(__dirname + "/avro/taskmanager/messages/commands/AvroDispatchTask.avsc", { registry });
var retryTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/commands/AvroRetryTask.avsc", { registry });
var retryTaskAttemptType = avro.parse(__dirname +"/avro/taskmanager/messages/commands/AvroRetryTaskAttempt.avsc", { registry });

var taskAttemptCompletedType = avro.parse(__dirname +"/avro/taskmanager/messages/events/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptDispatchedType = avro.parse(__dirname +"/avro/taskmanager/messages/events/AvroTaskAttemptDispatched.avsc", { registry });
var taskAttemptFailedType = avro.parse(__dirname +"/avro/taskmanager/messages/events/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptStartedType = avro.parse(__dirname +"/avro/taskmanager/messages/events/AvroTaskAttemptStarted.avsc", { registry });
var taskCanceledType = avro.parse(__dirname +"/avro/taskmanager/messages/events/AvroTaskCanceled.avsc", { registry });

var taskMessageType = avro.parse(__dirname +"/avro/taskmanager/messages/AvroTaskMessage.avsc", { registry  });
var runTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/AvroRunTask.avsc", { registry  });


module.exports = {
    assertValid,

    cancelTaskType,
    dispatchTaskType,
    retryTaskType,
    retryTaskAttemptType,

    taskAttemptCompletedType,
    taskAttemptDispatchedType,
    taskAttemptFailedType,
    taskAttemptStartedType,
    taskCanceledType,

    taskMessageType,
    runTaskType
}
