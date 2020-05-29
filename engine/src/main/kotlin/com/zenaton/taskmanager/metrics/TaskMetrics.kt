package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.messages.metrics.TaskMetricCreated
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
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

        if (message is TaskStatusUpdated) {
            when (message.oldStatus) {
                TaskStatus.RUNNING_OK -> newState.runningOkCount--
                TaskStatus.RUNNING_WARNING -> newState.runningWarningCount--
                TaskStatus.RUNNING_ERROR -> newState.runningErrorCount--
                TaskStatus.TERMINATED_COMPLETED -> newState.terminatedCompletedCount--
                TaskStatus.TERMINATED_CANCELED -> newState.terminatedCanceledCount--
                null -> Unit
            }

            when (message.newStatus) {
                TaskStatus.RUNNING_OK -> newState.runningOkCount++
                TaskStatus.RUNNING_WARNING -> newState.runningWarningCount++
                TaskStatus.RUNNING_ERROR -> newState.runningErrorCount++
                TaskStatus.TERMINATED_COMPLETED -> newState.terminatedCompletedCount++
                TaskStatus.TERMINATED_CANCELED -> newState.terminatedCanceledCount++
            }
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(message.taskName, newState, oldState)
        }

        // It's a new task type
        if (oldState == null) {
            val tsc = TaskMetricCreated(taskName = message.taskName)

            dispatcher.dispatch(tsc)
        }
    }
}
