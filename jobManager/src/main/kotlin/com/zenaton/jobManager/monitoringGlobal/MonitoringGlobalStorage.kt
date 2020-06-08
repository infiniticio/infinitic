package com.zenaton.jobManager.monitoringGlobal

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.interfaces.AvroStorage

class MonitoringGlobalStorage(val avroStorage: AvroStorage) {

    fun getState(): MonitoringGlobalState? {
        return avroStorage.getMonitoringGlobalState()?.let { AvroConverter.fromAvro(it) }
    }

    fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        avroStorage.updateMonitoringGlobalState(
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteState() {
        avroStorage.deleteMonitoringGlobalState()
    }
}
