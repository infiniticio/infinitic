package com.zenaton.taskManager.pulsar.engine

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.engine.EngineStorage
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.states.AvroTaskState
import org.apache.pulsar.functions.api.Context

/**
 * This class provides methods to access task's state
 */
class EnginePulsarStorage(val context: Context) : EngineStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = AvroConverter

    override fun getState(taskId: TaskId): EngineState? {
        return context.getState(taskId.id)?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskState>(it)) }
    }

    override fun updateState(taskId: TaskId, newState: EngineState, oldState: EngineState?) {
        context.putState(taskId.id, avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState(taskId: TaskId) {
        context.deleteState(taskId.id)
    }
}
