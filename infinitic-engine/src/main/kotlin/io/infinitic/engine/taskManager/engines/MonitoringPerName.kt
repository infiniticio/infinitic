package io.infinitic.engine.taskManager.engines

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.TaskCreated
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.engine.taskManager.storage.TaskStateStorage

class MonitoringPerName(
    val storage: TaskStateStorage,
    val dispatcher: Dispatcher
) {
    suspend fun handle(message: ForMonitoringPerNameMessage) {

        // get associated state
        val oldState = storage.getMonitoringPerNameState(message.taskName)
        val newState = oldState?.deepCopy() ?: MonitoringPerNameState(message.taskName)

        when (message) {
            is TaskStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateMonitoringPerNameState(message.taskName, newState, oldState)
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
