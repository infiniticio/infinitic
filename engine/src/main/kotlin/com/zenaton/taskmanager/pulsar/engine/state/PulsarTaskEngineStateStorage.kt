package com.zenaton.taskmanager.pulsar.engine.state

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.engine.state.TaskEngineState
import com.zenaton.taskmanager.engine.state.TaskEngineStateStorage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.states.AvroTaskState
import org.apache.pulsar.functions.api.Context

/**
 * This class provides methods to access task's state
 */
class PulsarTaskEngineStateStorage(val context: Context) : TaskEngineStateStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = TaskAvroConverter

    override fun getState(taskId: TaskId): TaskEngineState? {
        return context.getState(taskId.id)?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskState>(it)) }
    }

    override fun updateState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        context.putState(taskId.id, avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState(taskId: TaskId) {
        context.deleteState(taskId.id)
    }
}
