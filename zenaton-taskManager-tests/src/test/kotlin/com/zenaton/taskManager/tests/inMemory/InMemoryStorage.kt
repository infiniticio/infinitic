package com.zenaton.taskManager.tests.inMemory

import com.zenaton.taskManager.common.data.Task
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.states.AvroTaskEngineState
import com.zenaton.taskManager.states.AvroMonitoringGlobalState
import com.zenaton.taskManager.states.AvroMonitoringPerNameState

internal class InMemoryStorage : AvroStorage {
    var jobEngineStore: Map<String, AvroTaskEngineState> = mapOf()
    var monitoringPerNameStore: Map<String, AvroMonitoringPerNameState> = mapOf()
    var monitoringGlobalStore: AvroMonitoringGlobalState? = null

    fun isTerminated(task: Task): Boolean {
        return jobEngineStore[task.taskId.id] == null
    }

    fun reset() {
        jobEngineStore = mapOf()
        monitoringPerNameStore = mapOf()
        monitoringGlobalStore = null
    }

    override fun getTaskEngineState(jobId: String): AvroTaskEngineState? {
        return jobEngineStore[jobId]
    }

    override fun updateTaskEngineState(jobId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?) {
        jobEngineStore = jobEngineStore.plus(jobId to newState)
    }

    override fun deleteTaskEngineState(jobId: String) {
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
