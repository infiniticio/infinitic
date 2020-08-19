package com.zenaton.taskManager.engine.storages

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.TaskName
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.common.states.MonitoringPerNameState

class MonitoringPerNameStorage(val avroStorage: AvroStorage) {

    fun getState(taskName: TaskName): MonitoringPerNameState? {
        return avroStorage.getMonitoringPerNameState(taskName.name)?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        avroStorage.updateMonitoringPerNameState(
            taskName.name,
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState(taskName: TaskName) {
        avroStorage.deleteMonitoringPerNameState(taskName.name)
    }
}
