package com.zenaton.jobManager.monitoring.perName

import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.logger.Logger
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.interfaces.MonitoringPerNameMessage

class MonitoringPerNameEngine {
    lateinit var storage: MonitoringPerNameStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: MonitoringPerNameMessage) {
        // get associated state
        val oldState = storage.getState(message.jobName)
        val newState = oldState?.copy() ?: MonitoringPerNameState(message.jobName)

        when (message) {
            is JobStatusUpdated -> handleTaskStatusUpdated(message, newState)
            else -> throw Exception("Unknown MonitoringPerNameMessage: $message")
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(message.jobName, newState, oldState)
        }

        // It's a new task type
        if (oldState == null) {
            val tsc = JobCreated(jobName = message.jobName)

            dispatcher.toMonitoringGlobal(tsc)
        }
    }

    private fun handleTaskStatusUpdated(message: JobStatusUpdated, state: MonitoringPerNameState) {
        when (message.oldStatus) {
            JobStatus.RUNNING_OK -> state.runningOkCount--
            JobStatus.RUNNING_WARNING -> state.runningWarningCount--
            JobStatus.RUNNING_ERROR -> state.runningErrorCount--
            JobStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount--
            JobStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount--
            else -> Unit
        }

        when (message.newStatus) {
            JobStatus.RUNNING_OK -> state.runningOkCount++
            JobStatus.RUNNING_WARNING -> state.runningWarningCount++
            JobStatus.RUNNING_ERROR -> state.runningErrorCount++
            JobStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount++
            JobStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount++
        }
    }
}
