const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});
  
    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskEngineMessageType.avsc", { registry  });
var cancelTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroCancelTask.avsc", { registry });
var dispatchTaskType = avro.parse(__dirname + "/avro/taskmanager/messages/engine/AvroDispatchTask.avsc", { registry });
var retryTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroRetryTask.avsc", { registry });
var retryTaskAttemptType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroRetryTaskAttempt.avsc", { registry });
var taskAttemptCompletedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskAttemptCompleted.avsc", { registry });
var taskAttemptDispatchedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskAttemptDispatched.avsc", { registry });
var taskAttemptFailedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskAttemptFailed.avsc", { registry });
var taskAttemptStartedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskAttemptStarted.avsc", { registry });
var taskCanceledType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskCanceled.avsc", { registry });
var taskCompletedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskCompleted.avsc", { registry });
var taskDispatchedType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskDispatched.avsc", { registry });
var taskEngineMessageType = avro.parse(__dirname +"/avro/taskmanager/messages/engine/AvroTaskEngineMessage.avsc", { registry  });

avro.parse(__dirname +"/avro/taskmanager/messages/workers/AvroTaskworkerMessageType.avsc", { registry  });
var runTaskType = avro.parse(__dirname +"/avro/taskmanager/messages/workers/AvroRunTask.avsc", { registry  });
var taskWorkerMessageType = avro.parse(__dirname +"/avro/taskmanager/messages/workers/AvroTaskWorkerMessage.avsc", { registry  });


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
    taskCompletedType,
    taskDispatchedType,
    taskEngineMessageType,

    runTaskType,
    taskWorkerMessageType,
}
