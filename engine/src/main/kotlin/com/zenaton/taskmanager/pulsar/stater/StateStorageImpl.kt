package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.state.StateStorage
import com.zenaton.taskmanager.states.AvroTaskState
import org.apache.pulsar.functions.api.Context

class StateStorageImpl(private val context: Context) : StateStorage {
    var avroSerDe = AvroSerDe
    var avroConverter = TaskAvroConverter

    override fun getState(key: String): TaskState? = context.getState(key)?. let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskState>(it)) }

    override fun createState(key: String, state: TaskState) = context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))

    override fun updateState(key: String, state: TaskState) = context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))

    override fun deleteState(key: String) = context.deleteState(key)

    override fun incrCounter(key: String, amount: Long) = context.incrCounter(key, amount)
}
