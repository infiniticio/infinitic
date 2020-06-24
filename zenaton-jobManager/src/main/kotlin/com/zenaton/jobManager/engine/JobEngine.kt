package com.zenaton.jobManager.engine

import com.zenaton.common.data.interfaces.plus
import com.zenaton.jobManager.data.JobAttemptId
import com.zenaton.jobManager.data.JobAttemptRetry
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import org.slf4j.Logger

class JobEngine {
    lateinit var logger: Logger
    lateinit var storage: JobEngineStateStorage
    lateinit var dispatcher: Dispatcher

    fun handle(message: ForJobEngineMessage) {
        // immediately discard messages that are non managed
        when (message) {
            is JobAttemptDispatched -> return
            is JobAttemptStarted -> return
            is JobCompleted -> return
            is JobCanceled -> return
        }

        // get current state
        val oldState = storage.getState(message.jobId)

        if (oldState != null) {
            // discard message (except JobAttemptCompleted) if state has already evolved
            if (message is JobAttemptMessage && message !is JobAttemptCompleted) {
                if (oldState.jobAttemptId != message.jobAttemptId) return
                if (oldState.jobAttemptRetry != message.jobAttemptRetry) return
            }
        } else {
            // discard message if job is already completed
            if (message !is DispatchJob) return
        }

        val newState =
            if (oldState == null)
                dispatchJob(message as DispatchJob)
            else when (message) {
                is CancelJob -> cancelJob(oldState)
                is RetryJob -> retryJob(oldState)
                is RetryJobAttempt -> retryJobAttempt(oldState)
                is JobAttemptFailed -> jobAttemptFailed(oldState, message)
                is JobAttemptCompleted -> jobAttemptCompleted(oldState, message)
                else -> throw Exception("Unknown EngineMessage: $message")
            }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.jobStatus.isTerminated) {
            storage.updateState(message.jobId, newState, oldState)
        }

        // Send JobStatusUpdated if needed
        if (oldState?.jobStatus != newState.jobStatus) {
            val tsc = JobStatusUpdated(
                jobId = newState.jobId,
                jobName = newState.jobName,
                oldStatus = oldState?.jobStatus,
                newStatus = newState.jobStatus
            )

            dispatcher.toMonitoringPerName(tsc)
        }
    }

    private fun cancelJob(oldState: JobEngineState): JobEngineState {
        val state = oldState.copy(jobStatus = JobStatus.TERMINATED_CANCELED)

        // log event
        val tad = JobCanceled(
            jobId = state.jobId,
            jobMeta = state.jobMeta
        )
        dispatcher.toJobEngine(tad)

        // Delete stored state
        storage.deleteState(state.jobId)

        return state
    }

    private fun dispatchJob(msg: DispatchJob): JobEngineState {
        // init a state
        val state = JobEngineState(
            jobId = msg.jobId,
            jobName = msg.jobName,
            jobInput = msg.jobInput,
            jobAttemptId = JobAttemptId(),
            jobStatus = JobStatus.RUNNING_OK,
            jobMeta = msg.jobMeta
        )

        // send job to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobInput = state.jobInput
        )
        dispatcher.toWorkers(rt)

        // log events
        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex
        )
        dispatcher.toJobEngine(tad)

        return state
    }

    private fun retryJob(oldState: JobEngineState): JobEngineState {
        val state = oldState.copy(
            jobStatus = JobStatus.RUNNING_WARNING,
            jobAttemptId = JobAttemptId(),
            jobAttemptRetry = JobAttemptRetry(0),
            jobAttemptIndex = oldState.jobAttemptIndex + 1
        )

        // send job to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobInput = state.jobInput
        )
        dispatcher.toWorkers(rt)

        // log event
        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex
        )
        dispatcher.toJobEngine(tad)

        return state
    }

    private fun retryJobAttempt(oldState: JobEngineState): JobEngineState {
        val state = oldState.copy(
            jobStatus = JobStatus.RUNNING_WARNING,
            jobAttemptRetry = oldState.jobAttemptRetry + 1
        )

        // send job to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobInput = state.jobInput
        )
        dispatcher.toWorkers(rt)

        // log event
        val tar = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex,
            jobAttemptRetry = state.jobAttemptRetry
        )
        dispatcher.toJobEngine(tar)

        return state
    }

    private fun jobAttemptCompleted(oldState: JobEngineState, msg: JobAttemptCompleted): JobEngineState {
        val state = oldState.copy(jobStatus = JobStatus.TERMINATED_COMPLETED)

        // log event
        val tc = JobCompleted(
            jobId = state.jobId,
            jobOutput = msg.jobOutput,
            jobMeta = state.jobMeta
        )
        dispatcher.toJobEngine(tc)

        // Delete stored state
        storage.deleteState(state.jobId)

        return state
    }

    private fun jobAttemptFailed(oldState: JobEngineState, msg: JobAttemptFailed): JobEngineState {
        return delayRetryJobAttempt(oldState, delay = msg.jobAttemptDelayBeforeRetry)
    }

    private fun delayRetryJobAttempt(oldState: JobEngineState, delay: Float?): JobEngineState {
        // no retry
        if (delay == null) return oldState.copy(jobStatus = JobStatus.RUNNING_ERROR)
        // immediate retry
        if (delay <= 0f) return retryJobAttempt(oldState)
        // delayed retry
        val state = oldState.copy(jobStatus = JobStatus.RUNNING_WARNING)

        // schedule next attempt
        val tar = RetryJobAttempt(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex
        )
        dispatcher.toJobEngine(tar, after = delay)

        return state
    }
}
