package com.zenaton.taskManager.pulsar.engine

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.taskManager.data.JobId
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.engine.EngineStorage
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import org.apache.pulsar.functions.api.Context

/**
 * This class provides methods to access task's state
 */
class EnginePulsarStorage(val context: Context) : EngineStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = AvroConverter

    override fun getState(jobId: JobId): EngineState? {
        return context.getState(jobId.id)?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroEngineState>(it)) }
    }

    override fun updateState(jobId: JobId, newState: EngineState, oldState: EngineState?) {
        context.putState(jobId.id, avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState(jobId: JobId) {
        context.deleteState(jobId.id)
    }
}
