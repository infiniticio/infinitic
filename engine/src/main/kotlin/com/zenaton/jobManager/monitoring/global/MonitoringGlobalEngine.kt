package com.zenaton.jobManager.monitoring.global

import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.logger.Logger

class MonitoringGlobalEngine {
    lateinit var storage: MonitoringGlobalStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: MonitoringGlobalMessage) {
        // get associated state
        val oldState = storage.getState()
        val newState = oldState?.copy() ?: MonitoringGlobalState()

        when (message) {
            is JobCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(newState, oldState)
        }
    }

    private fun handleTaskTypeCreated(message: JobCreated, state: MonitoringGlobalState) {
        val added = state.jobNames.add(message.jobName)

        if (!added) logger.warn("Trying to add a task %s already known in state %s", message.jobName, state)
    }
}
