package com.zenaton.jobManager.pulsar.engine

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.engine.EngineStorage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.states.AvroEngineState
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
