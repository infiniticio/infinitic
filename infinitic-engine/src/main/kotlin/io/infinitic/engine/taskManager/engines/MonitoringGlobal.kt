package io.infinitic.engine.taskManager.engines

import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.TaskCreated
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.engine.taskManager.storage.TaskStateStorage

class MonitoringGlobal(
    val storage: TaskStateStorage
) {
    fun handle(message: ForMonitoringGlobalMessage) {

        // get associated state
        val oldState = storage.getMonitoringGlobalState()
        val newState = oldState?.deepCopy() ?: MonitoringGlobalState()

        when (message) {
            is TaskCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateMonitoringGlobalState(newState, oldState)
        }
    }

    private fun handleTaskTypeCreated(message: TaskCreated, state: MonitoringGlobalState) {
        state.taskNames.add(message.taskName)
    }
}
