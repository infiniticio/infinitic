package io.infinitic.taskManager.engine.engines

import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.TaskCreated
import io.infinitic.taskManager.common.states.MonitoringGlobalState
import io.infinitic.taskManager.engine.storage.StateStorage
import org.slf4j.Logger

class MonitoringGlobal(
    val storage: StateStorage
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
