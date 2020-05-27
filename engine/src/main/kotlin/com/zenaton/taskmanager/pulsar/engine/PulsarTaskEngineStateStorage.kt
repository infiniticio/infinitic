package com.zenaton.taskmanager.pulsar.engine

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.engine.TaskEngineStateStorage
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

    override fun getState(taskId: TaskId): TaskState? {
        return context.getState(taskId.id)?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskState>(it)) }
    }

    override fun updateState(taskId: TaskId, newState: TaskState, oldState: TaskState?) {
        context.putState(taskId.id, avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState(taskId: TaskId) {
        context.deleteState(taskId.id)
    }
}
