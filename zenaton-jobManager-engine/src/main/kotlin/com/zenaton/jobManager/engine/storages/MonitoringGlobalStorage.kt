package com.zenaton.jobManager.engine.storages

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.avroInterfaces.AvroStorage
import com.zenaton.jobManager.common.states.MonitoringGlobalState

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
