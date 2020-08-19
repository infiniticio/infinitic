package io.infinitic.taskManager.engine.storages

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.common.states.MonitoringPerNameState

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
