package io.infinitic.taskManager.tests.inMemory

import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage as AvroTaskStorage
import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.engine.avroInterfaces.AvroStorage as AvroWorkflowStorage
import io.infinitic.workflowManager.states.AvroWorkfloState

internal class InMemoryStorage : AvroTaskStorage, AvroWorkflowStorage {
    var workflowEngineStore: Map<String, AvroWorkfloState> = mapOf()
    var taskEngineStore: Map<String, AvroTaskEngineState> = mapOf()
    var monitoringPerNameStore: Map<String, AvroMonitoringPerNameState> = mapOf()
    var monitoringGlobalStore: AvroMonitoringGlobalState? = null

    fun isTerminated(workflowInstance: WorkflowInstance): Boolean {
        return workflowEngineStore[workflowInstance.workflowId.id] == null
    }

    fun isTerminated(taskInstance: TaskInstance): Boolean {
        return taskEngineStore[taskInstance.taskId.id] == null
    }

    fun reset() {
        workflowEngineStore = mapOf()
        taskEngineStore = mapOf()
        monitoringPerNameStore = mapOf()
        monitoringGlobalStore = null
    }

    override fun getWorkflowState(workflowId: String): AvroWorkfloState? {
        return workflowEngineStore[workflowId]
    }

    override fun updateWorkflowState(workflowId: String, newState: AvroWorkfloState, oldState: AvroWorkfloState?) {
        workflowEngineStore = workflowEngineStore.plus(workflowId to newState)
    }

    override fun deleteWorkflowState(workflowId: String) {
        workflowEngineStore = workflowEngineStore.minus(workflowId)
    }

    override fun getTaskEngineState(taskId: String): AvroTaskEngineState? {
        return taskEngineStore[taskId]
    }

    override fun updateTaskEngineState(taskId: String, newState: AvroTaskEngineState, oldState: AvroTaskEngineState?) {
        taskEngineStore = taskEngineStore.plus(taskId to newState)
    }

    override fun deleteTaskEngineState(taskId: String) {
        taskEngineStore = taskEngineStore.minus(taskId)
    }

    override fun getMonitoringPerNameState(taskName: String): AvroMonitoringPerNameState? = monitoringPerNameStore[taskName]

    override fun updateMonitoringPerNameState(taskName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        monitoringPerNameStore = monitoringPerNameStore.plus(taskName to newState)
    }

    override fun deleteMonitoringPerNameState(taskName: String) {
        monitoringPerNameStore = monitoringPerNameStore.minus(taskName)
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? = monitoringGlobalStore

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        monitoringGlobalStore = newState
    }

    override fun deleteMonitoringGlobalState() {
        monitoringGlobalStore = null
    }
}
