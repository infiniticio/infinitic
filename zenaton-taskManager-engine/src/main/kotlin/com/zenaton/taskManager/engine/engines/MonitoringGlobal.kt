package com.zenaton.taskManager.engine.engines

import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.JobCreated
import com.zenaton.taskManager.common.states.MonitoringGlobalState
import com.zenaton.taskManager.engine.storages.MonitoringGlobalStorage
import org.slf4j.Logger

class MonitoringGlobal {
    lateinit var logger: Logger
    lateinit var storage: MonitoringGlobalStorage

    fun handle(message: ForMonitoringGlobalMessage) {

        // get associated state
        val oldState = storage.getState()
        val newState = oldState?.deepCopy() ?: MonitoringGlobalState()

        when (message) {
            is JobCreated -> handleTaskTypeCreated(message, newState)
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
