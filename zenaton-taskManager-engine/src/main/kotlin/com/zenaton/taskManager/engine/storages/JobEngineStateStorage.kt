package com.zenaton.taskManager.engine.storages

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.JobId
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.common.states.JobEngineState

class JobEngineStateStorage(val avroStorage: AvroStorage) {

    fun getState(jobId: JobId): JobEngineState? {
        return avroStorage.getJobEngineState(jobId.id)?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(jobId: JobId, newState: JobEngineState, oldState: JobEngineState?) {
        avroStorage.updateJobEngineState(
            jobId.id,
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState(jobId: JobId) {
        avroStorage.deleteJobEngineState(jobId.id)
    }
}
