package com.zenaton.taskmanager.pulsar.admin.state

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.admin.state.TaskAdminState
import com.zenaton.taskmanager.admin.state.TaskAdminStateStorage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import org.apache.pulsar.functions.api.Context

class PulsarTaskAdminStateStorage(val context: Context) : TaskAdminStateStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = TaskAvroConverter

    override fun getState(): TaskAdminState? {
//        return context.getState(getStateKey())?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskAdminState>(it)) }
        return null
    }

    override fun updateState(newState: TaskAdminState, oldState: TaskAdminState?) {
        context.putState(getStateKey(), avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState() {
        context.deleteState(getStateKey())
    }

    fun getStateKey() = "metrics.task.counters"
}
