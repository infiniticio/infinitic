const avro = require('avro-js');

const assertValid = function assertValid(type, val) {
    return type.isValid(val, {errorHook: hook});

    function hook(path, any) {
      throw new Error(util.format('invalid %s: %j', path.join(), any));
    }
}

var registry = {}

// Engine
var cancelJob = avro.parse(__dirname +"/avro/jobManager/messages/AvroCancelJob.avsc", { registry });
var dispatchJob = avro.parse(__dirname + "/avro/jobManager/messages/AvroDispatchJob.avsc", { registry });
var retryJob = avro.parse(__dirname +"/avro/jobManager/messages/AvroRetryJob.avsc", { registry });
var retryJobAttempt = avro.parse(__dirname +"/avro/jobManager/messages/AvroRetryJobAttempt.avsc", { registry });
var JobAttemptCompleted = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobAttemptCompleted.avsc", { registry });
var JobAttemptDispatched = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobAttemptDispatched.avsc", { registry });
var JobAttemptFailed = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobAttemptFailed.avsc", { registry });
var JobAttemptStarted = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobAttemptStarted.avsc", { registry });
var JobCanceled = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobCanceled.avsc", { registry });
var JobCompleted = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobCompleted.avsc", { registry });
var forEngineMessage = avro.parse(__dirname +"/avro/jobManager/messages/envelopes/AvroForJobEngineMessage.avsc", { registry  });

// Workers
var runJob = avro.parse(__dirname +"/avro/jobManager/messages/AvroRunJob.avsc", { registry });
var forWorkerMessage = avro.parse(__dirname +"/avro/jobManager/messages/envelopes/AvroForWorkerMessage.avsc", { registry  });

// MonitoringPerName
avro.parse(__dirname +"/avro/jobManager/data/AvroJobStatus.avsc", { registry });
var jobStatusUpdated = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobStatusUpdated.avsc", { registry });
var forMonitoringPerNameMessage = avro.parse(__dirname +"/avro/jobManager/messages/envelopes/AvroForMonitoringPerNameMessage.avsc", { registry  });

// MonitoringGlobal
var jobCreated = avro.parse(__dirname +"/avro/jobManager/messages/AvroJobCreated.avsc", { registry });
var forMonitoringGlobalMessage = avro.parse(__dirname +"/avro/jobManager/messages/envelopes/AvroForMonitoringGlobalMessage.avsc", { registry  });

module.exports = {
    assertValid,

    cancelJob,
    dispatchJob,
    retryJob,
    retryJobAttempt,
    JobAttemptCompleted,
    JobAttemptDispatched,
    JobAttemptFailed,
    JobAttemptStarted,
    forEngineMessage,

    runJob,
    forWorkerMessage,

    jobStatusUpdated,
    forMonitoringPerNameMessage,

    jobCreated,
    forMonitoringGlobalMessage,
}
