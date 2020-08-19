package com.zenaton.taskManager.engine.storages

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.common.states.MonitoringGlobalState

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
