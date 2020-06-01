package com.zenaton.taskManager.monitoring.perName

import com.zenaton.taskManager.monitoring.global.TaskCreated
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger

class MonitoringPerNameEngine {
    lateinit var storage: MonitoringPerNameStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: MonitoringPerNameMessage) {
        // get associated state
        val oldState = storage.getState(message.taskName)
        val newState = oldState?.copy() ?: MonitoringPerNameState(message.taskName)

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

            dispatcher.dispatch(tsc)
        }
    }

    private fun handleTaskStatusUpdated(message: TaskStatusUpdated, state: MonitoringPerNameState) {
        when (message.oldStatus) {
            TaskStatus.RUNNING_OK -> state.runningOkCount--
            TaskStatus.RUNNING_WARNING -> state.runningWarningCount--
            TaskStatus.RUNNING_ERROR -> state.runningErrorCount--
            TaskStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount--
            TaskStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount--
            null -> Unit
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
