package com.zenaton.jobManager.tests.inMemory

import com.zenaton.jobManager.engine.avroInterfaces.AvroStorage
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState

internal class InMemoryStorage : AvroStorage {
    var jobEngineStore: Map<String, AvroJobEngineState> = mapOf()
    var monitoringPerNameStore: Map<String, AvroMonitoringPerNameState> = mapOf()
    var monitoringGlobalStore: AvroMonitoringGlobalState? = null

    fun reset() {
        jobEngineStore = mapOf()
        monitoringPerNameStore = mapOf()
        monitoringGlobalStore = null
    }

    override fun getJobEngineState(jobId: String): AvroJobEngineState? {
        return jobEngineStore[jobId]
    }

    override fun updateJobEngineState(jobId: String, newState: AvroJobEngineState, oldState: AvroJobEngineState?) {
        jobEngineStore = jobEngineStore.plus(jobId to newState)
    }

    override fun deleteJobEngineState(jobId: String) {
        jobEngineStore = jobEngineStore.minus(jobId)
    }

    override fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState? = monitoringPerNameStore[jobName]

    override fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        monitoringPerNameStore = monitoringPerNameStore.plus(jobName to newState)
    }

    override fun deleteMonitoringPerNameState(jobName: String) {
        monitoringPerNameStore = monitoringPerNameStore.minus(jobName)
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? = monitoringGlobalStore

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        monitoringGlobalStore = newState
    }

    override fun deleteMonitoringGlobalState() {
        monitoringGlobalStore = null
    }
}
