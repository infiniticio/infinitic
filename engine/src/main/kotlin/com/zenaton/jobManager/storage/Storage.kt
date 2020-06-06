package com.zenaton.jobManager.storage

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.states.EngineState
import com.zenaton.jobManager.states.MonitoringGlobalState
import com.zenaton.jobManager.states.MonitoringPerNameState

class Storage(val avroStorage: AvroStorage) {

    fun getEngineState(jobId: JobId): EngineState? {
        return avroStorage.getEngineState(jobId.id)?.let { AvroConverter.fromAvro(it) }
    }

    fun updateEngineState(jobId: JobId, newState: EngineState, oldState: EngineState?) {
        avroStorage.updateEngineState(
            jobId.id,
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteEngineState(jobId: JobId) {
        avroStorage.deleteEngineState(jobId.id)
    }

    fun getMonitoringPerNameState(jobName: JobName): MonitoringPerNameState? {
        return avroStorage.getMonitoringPerNameState(jobName.name)?.let { AvroConverter.fromAvro(it) }
    }

    fun updateMonitoringPerNameState(jobName: JobName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?) {
        avroStorage.updateMonitoringPerNameState(
            jobName.name,
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteMonitoringPerNameState(jobName: JobName) {
        avroStorage.deleteMonitoringPerNameState(jobName.name)
    }

    fun getMonitoringGlobalState(): MonitoringGlobalState? {
        return avroStorage.getMonitoringGlobalState()?.let { AvroConverter.fromAvro(it) }
    }

    fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?) {
        avroStorage.updateMonitoringGlobalState(
            AvroConverter.toAvro(newState),
            oldState?.let { AvroConverter.toAvro(it) }
        )
    }

    fun deleteMonitoringGlobalState() {
        avroStorage.deleteMonitoringGlobalState()
    }
}
