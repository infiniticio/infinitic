package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.states.AvroTaskMetricsState
import org.apache.pulsar.functions.api.Context

class PulsarTaskMetricsStateStorage(val context: Context) : TaskMetricsStateStorage {
    var avroSerDe = AvroSerDe
    var avroConverter = TaskAvroConverter

    override fun getState(key: String): TaskMetricsState? = context.getState(key)?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskMetricsState>(it)) }

    override fun putState(key: String, state: TaskMetricsState) = context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))

    override fun deleteState(key: String) = context.deleteState(key)

    override fun incrCounter(key: String, amount: Long) = context.incrCounter(key, amount)

    override fun getCounter(key: String) = context.getCounter(key)
}
