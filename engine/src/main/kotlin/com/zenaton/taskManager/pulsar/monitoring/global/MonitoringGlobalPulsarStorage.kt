package com.zenaton.taskManager.pulsar.monitoring.global

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalStorage
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalState
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.states.AvroTaskAdminState
import org.apache.pulsar.functions.api.Context

class MonitoringGlobalPulsarStorage(val context: Context) : MonitoringGlobalStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = AvroConverter

    override fun getState(): MonitoringGlobalState? {
        return context.getState(getStateKey())?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroTaskAdminState>(it)) }
    }

    override fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        context.putState(getStateKey(), avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState() {
        context.deleteState(getStateKey())
    }

    fun getStateKey() = "metrics.task.counters"
}
