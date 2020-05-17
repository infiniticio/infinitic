package com.zenaton.taskmanager.pulsar.stater

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.taskmanager.states.AvroTaskState
import com.zenaton.workflowengine.interfaces.StaterInterface
import org.apache.pulsar.functions.api.Context

/**
 * This class provides methods to access task's state
 */
class TaskStater(private val context: Context) : StaterInterface<TaskState> {
    // serializer injection
    var avroSerDe = AvroSerDe
    // converter injection
    var avroConverter = TaskAvroConverter

    override fun getState(key: String): TaskState? {
        return context.getState(key)?. let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskState>(it)) }
    }

    override fun createState(key: String, state: TaskState) {
        context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun updateState(key: String, state: TaskState) {
        context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun deleteState(key: String) {
        context.deleteState(key)
    }
}
