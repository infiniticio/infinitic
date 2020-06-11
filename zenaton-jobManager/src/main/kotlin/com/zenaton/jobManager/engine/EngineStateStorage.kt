package com.zenaton.jobManager.engine

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.interfaces.AvroStorage

class EngineStateStorage(val avroStorage: AvroStorage) {

    fun getState(jobId: JobId): EngineState? {
        return avroStorage.getEngineState(jobId.id)?.let { AvroConverter.fromAvro(it) }
    }

    fun updateState(jobId: JobId, newState: EngineState, oldState: EngineState?) {
        avroStorage.updateEngineState(
            jobId.id,
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteState(jobId: JobId) {
        avroStorage.deleteEngineState(jobId.id)
    }
}
