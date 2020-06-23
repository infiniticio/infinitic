package com.zenaton.jobManager.monitoringPerName

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.interfaces.AvroStorage

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
