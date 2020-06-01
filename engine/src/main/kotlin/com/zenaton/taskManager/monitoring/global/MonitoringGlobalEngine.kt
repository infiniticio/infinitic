package com.zenaton.taskManager.monitoring.global

import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger

class MonitoringGlobalEngine {
    lateinit var storage: MonitoringGlobalStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: MonitoringGlobalMessage) {
        // get associated state
        val oldState = storage.getState()
        val newState = oldState?.copy() ?: MonitoringGlobalState()

        when (message) {
            is TaskCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(newState, oldState)
        }
    }

    private fun handleTaskTypeCreated(message: TaskCreated, state: MonitoringGlobalState) {
        val added = state.taskNames.add(message.taskName)

        if (!added) logger.warn("Trying to add a task %s already known in state %s", message.taskName, state)
    }
}
