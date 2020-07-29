package com.zenaton.jobManager.storages

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.common.states.MonitoringPerNameState

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
