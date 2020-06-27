package com.zenaton.jobManager.storages

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.states.MonitoringGlobalState

class MonitoringGlobalStorage(val avroStorage: AvroStorage) {

    fun getState(): MonitoringGlobalState? {
        return avroStorage.getMonitoringGlobalState()?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        avroStorage.updateMonitoringGlobalState(
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState() {
        avroStorage.deleteMonitoringGlobalState()
    }
}
