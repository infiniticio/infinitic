package com.zenaton.pulsar.topics.tasks

import com.zenaton.engine.interfaces.StaterInterface
import com.zenaton.engine.topics.tasks.state.TaskState
import com.zenaton.pulsar.utils.AvroConverter
import com.zenaton.pulsar.utils.AvroSerDe
import com.zenaton.states.AvroTaskState
import org.apache.pulsar.functions.api.Context

class TaskStater(private val context: Context) : StaterInterface<TaskState> {
    // serializer injection
    var avroSerDe = AvroSerDe
    // converter injection
    var avroConverter = AvroConverter

    override fun getState(key: String): TaskState? {
        return context.getState(key) ?. let { avroConverter.fromAvro(avroSerDe.deserialize(it, AvroTaskState::class)) }
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
