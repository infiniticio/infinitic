package com.zenaton.jobManager.pulsar.monitoring.global

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalStorage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import org.apache.pulsar.functions.api.Context

class MonitoringGlobalPulsarStorage(val context: Context) : MonitoringGlobalStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    // converter injection
    var avroConverter = AvroConverter

    override fun getState(): MonitoringGlobalState? {
        return context.getState(getStateKey())?.let { avroConverter.fromAvro(avroSerDe.deserialize<AvroMonitoringGlobalState>(it)) }
    }

    override fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        context.putState(getStateKey(), avroSerDe.serialize(avroConverter.toAvro(newState)))
    }

    override fun deleteState() {
        context.deleteState(getStateKey())
    }

    fun getStateKey() = "metrics.task.counters"
}
