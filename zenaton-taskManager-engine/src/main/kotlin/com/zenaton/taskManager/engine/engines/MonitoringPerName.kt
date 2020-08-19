package com.zenaton.taskManager.engine.engines

import com.zenaton.taskManager.common.data.TaskStatus
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.TaskCreated
import com.zenaton.taskManager.common.messages.TaskStatusUpdated
import com.zenaton.taskManager.common.states.MonitoringPerNameState
import com.zenaton.taskManager.engine.storages.MonitoringPerNameStorage
import org.slf4j.Logger

class MonitoringPerName {
    lateinit var logger: Logger
    lateinit var storage: MonitoringPerNameStorage
    lateinit var dispatcher: Dispatcher

    fun handle(message: ForMonitoringPerNameMessage) {

        // get associated state
        val oldState = storage.getState(message.taskName)
        val newState = oldState?.deepCopy() ?: MonitoringPerNameState(message.taskName)

        when (message) {
            is TaskStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(message.taskName, newState, oldState)
        }

        // It's a new task type
        if (oldState == null) {
            val tsc = TaskCreated(taskName = message.taskName)

            dispatcher.toMonitoringGlobal(tsc)
        }
    }

    private fun handleTaskStatusUpdated(message: TaskStatusUpdated, state: MonitoringPerNameState) {
        when (message.oldStatus) {
            TaskStatus.RUNNING_OK -> state.runningOkCount--
            TaskStatus.RUNNING_WARNING -> state.runningWarningCount--
            TaskStatus.RUNNING_ERROR -> state.runningErrorCount--
            TaskStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount--
            TaskStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount--
            else -> Unit
        }

        when (message.newStatus) {
            TaskStatus.RUNNING_OK -> state.runningOkCount++
            TaskStatus.RUNNING_WARNING -> state.runningWarningCount++
            TaskStatus.RUNNING_ERROR -> state.runningErrorCount++
            TaskStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount++
            TaskStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount++
        }
    }
}
