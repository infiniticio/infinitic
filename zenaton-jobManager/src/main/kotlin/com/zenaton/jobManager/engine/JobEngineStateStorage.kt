package com.zenaton.jobManager.engine

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.interfaces.AvroStorage

class JobEngineStateStorage(val avroStorage: AvroStorage) {

    fun getState(jobId: JobId): JobEngineState? {
        return avroStorage.getJobEngineState(jobId.id)?.let { AvroConverter.fromAvro(it) }
    }

    fun updateState(jobId: JobId, newState: JobEngineState, oldState: JobEngineState?) {
        avroStorage.updateJobEngineState(
            jobId.id,
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteState(jobId: JobId) {
        avroStorage.deleteJobEngineState(jobId.id)
    }
}
