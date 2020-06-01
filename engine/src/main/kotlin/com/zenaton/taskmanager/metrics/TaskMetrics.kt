package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.admin.messages.TaskTypeCreated
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage

class TaskMetrics {
    lateinit var storage: TaskMetricsStateStorage
    lateinit var dispatcher: TaskDispatcher
    lateinit var logger: TaskLogger

    fun handle(message: TaskMetricMessage) {
        // get associated state
        val oldState = storage.getState(message.taskName)
        val newState = oldState?.copy() ?: TaskMetricsState(message.taskName)

        when (message) {
            is TaskStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(message.taskName, newState, oldState)
        }

        // It's a new task type
        if (oldState == null) {
            val tsc = TaskTypeCreated(taskName = message.taskName)

            dispatcher.dispatch(tsc)
        }
    }

    private fun handleTaskStatusUpdated(message: TaskStatusUpdated, state: TaskMetricsState) {
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
