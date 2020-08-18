package com.zenaton.jobManager.engine.avroInterfaces

import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState

interface AvroStorage {
    fun getJobEngineState(jobId: String): AvroJobEngineState?
    fun updateJobEngineState(jobId: String, newState: AvroJobEngineState, oldState: AvroJobEngineState?)
    fun deleteJobEngineState(jobId: String)

    fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState?
    fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?)
    fun deleteMonitoringPerNameState(jobName: String)

    fun getMonitoringGlobalState(): AvroMonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?)
    fun deleteMonitoringGlobalState()
}
