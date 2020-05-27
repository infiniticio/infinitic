package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage

class TaskMetrics {
    lateinit var stateStorage: TaskMetricsStateStorage

    fun handle(message: TaskMetricMessage) {
        if (message is TaskStatusUpdated) {
            decrementOldCounter(message)
            incrementNewCounter(message)
            persistCountersInState(message.taskName)
        }
    }

    private fun decrementOldCounter(message: TaskStatusUpdated) =
        message.oldStatus?.let {
            stateStorage.incrCounter(getCounterKey(message.taskName, it), -1)
        }

    private fun incrementNewCounter(message: TaskStatusUpdated) =
        stateStorage.incrCounter(getCounterKey(message.taskName, message.newStatus), 1)

    private fun persistCountersInState(taskName: TaskName) {
        val state = stateStorage.getState(getStateKey(taskName)) ?: TaskMetricsState(taskName)

        state.runningOkCount = stateStorage.getCounter(getCounterKey(taskName, TaskStatus.RUNNING_OK))
        state.runningWarningCount = stateStorage.getCounter(getCounterKey(taskName, TaskStatus.RUNNING_WARNING))
        state.runningErrorCount = stateStorage.getCounter(getCounterKey(taskName, TaskStatus.RUNNING_ERROR))
        state.terminatedCompletedCount = stateStorage.getCounter(getCounterKey(taskName, TaskStatus.TERMINATED_COMPLETED))
        state.terminatedCanceledCount = stateStorage.getCounter(getCounterKey(taskName, TaskStatus.TERMINATED_CANCELED))

        stateStorage.putState(getStateKey(taskName), state)
    }

    private fun getCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "metrics.rt.counter.task.${taskName.name.toLowerCase()}.${taskStatus.toString().toLowerCase()}"
    private fun getStateKey(taskName: TaskName) = "metrics.task.${taskName.name.toLowerCase()}.counters"
}
