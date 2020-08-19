package com.zenaton.taskManager.engine.storages

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.JobName
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.common.states.MonitoringPerNameState

class MonitoringPerNameStorage(val avroStorage: AvroStorage) {

    fun getState(jobName: JobName): MonitoringPerNameState? {
        return avroStorage.getMonitoringPerNameState(jobName.name)?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(jobName: JobName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        avroStorage.updateMonitoringPerNameState(
            jobName.name,
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState(jobName: JobName) {
        avroStorage.deleteMonitoringPerNameState(jobName.name)
    }
}
