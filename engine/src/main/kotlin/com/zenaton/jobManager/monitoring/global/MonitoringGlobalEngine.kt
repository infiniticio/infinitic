package com.zenaton.jobManager.monitoring.global

import com.zenaton.commons.data.interfaces.deepCopy
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import org.slf4j.Logger

class MonitoringGlobalEngine {
    lateinit var storage: MonitoringGlobalStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: ForMonitoringGlobalMessage) {
        // get associated state
        val oldState = storage.getState()
        val newState = oldState?.deepCopy() ?: MonitoringGlobalState()

        when (message) {
            is JobCreated -> handleTaskTypeCreated(message, newState)
            else -> throw Exception("Unknown MonitoringGlobalMessage: $message")
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(newState, oldState)
            logger.info("MonitoringPerNameState: from%s to%s", oldState, newState)
        }
    }

    private fun handleTaskTypeCreated(message: JobCreated, state: MonitoringGlobalState) {
        val added = state.jobNames.add(message.jobName)

        if (!added) logger.warn("Trying to add a task %s already known in state %s", message.jobName, state)
    }
}