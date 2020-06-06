package com.zenaton.jobManager.engine

import com.zenaton.commons.data.interfaces.deepCopy
import com.zenaton.commons.data.interfaces.inc
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
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as JobCompletedInWorkflow
import org.slf4j.Logger

class Engine {
    lateinit var logger: Logger
    lateinit var storage: EngineStorage
    lateinit var dispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcherInterface

    fun handle(message: ForEngineMessage) {

        // get associated state
        val oldState = storage.getState(message.jobId)
        var newState: EngineState? = oldState?.deepCopy()

        if (newState == null) {
            // a null state should mean that this task is already terminated => all messages others than JobDispatched are ignored
            if (message !is DispatchJob) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", message, null)
                return
            }
            // init a state
            newState = EngineState(
                jobId = message.jobId,
                jobName = message.jobName,
                jobData = message.jobData,
                workflowId = message.workflowId,
                jobAttemptId = JobAttemptId(),
                jobStatus = JobStatus.RUNNING_OK
            )
        } else {
            // this should never happen
            if (newState.jobId != message.jobId) {
                logger.error("Inconsistent taskId in message:%s and State:%s)", message, newState)
                return
            }
            // a non-null state with JobDispatched should mean that this message has been replicated
            if (message is DispatchJob) {
                logger.error("Already existing state:%s for message:%s", message, newState)
                return
            }
            // check taskAttemptId and taskAttemptIndex consistency
            if (message is JobAttemptMessage && message !is JobAttemptCompleted) {
                if (newState.jobAttemptId != message.jobAttemptId) {
                    logger.warn("Inconsistent jobAttemptId in message: (Can happen if the job has been manually retried)%s and State:%s", message, newState)
                    return
                }
                if (newState.jobAttemptRetry != message.jobAttemptRetry) {
                    logger.warn("Inconsistent jobAttemptIndex in message: (Can happen if this job has had timeout)%s and State:%s", message, newState)
                    return
                }
            }
        }

        when (message) {
            is CancelJob -> cancelJob(newState, message)
            is DispatchJob -> dispatchJob(newState)
            is RetryJob -> retryJob(newState)
            is RetryJobAttempt -> retryJobAttempt(newState)
            is JobAttemptDispatched -> Unit
            is JobAttemptStarted -> Unit
            is JobAttemptFailed -> taskAttemptFailed(newState, message)
            is JobAttemptCompleted -> taskAttemptCompleted(newState, message)
            is JobCompleted -> Unit
            is JobCanceled -> Unit
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

    private fun cancelJob(state: EngineState, msg: CancelJob) {
        state.jobStatus = JobStatus.TERMINATED_CANCELED

        // log event
        val tad = JobCanceled(
            jobId = msg.jobId
        )
        dispatcher.toEngine(tad)

        // Delete stored state
        storage.deleteState(state.jobId)
    }

    private fun dispatchJob(state: EngineState) {
        state.jobStatus = JobStatus.RUNNING_OK

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        dispatcher.toWorkers(rt)

        // log events
        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex
        )
        dispatcher.toEngine(tad)
    }

    private fun retryJob(state: EngineState) {
        state.jobStatus = JobStatus.RUNNING_WARNING
        state.jobAttemptId = JobAttemptId()
        state.jobAttemptRetry = JobAttemptRetry(0)
        state.jobAttemptIndex++

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        dispatcher.toWorkers(rt)

        // log event
        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex
        )
        dispatcher.toEngine(tad)
    }

    private fun retryJobAttempt(state: EngineState) {
        state.jobStatus = JobStatus.RUNNING_WARNING
        state.jobAttemptRetry++

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptRetry = state.jobAttemptRetry,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        dispatcher.toWorkers(rt)

        // log event
        val tar = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex,
            jobAttemptRetry = state.jobAttemptRetry
        )
        dispatcher.toEngine(tar)
    }

    private fun taskAttemptCompleted(state: EngineState, msg: JobAttemptCompleted) {
        state.jobStatus = JobStatus.TERMINATED_COMPLETED

        // if this task belongs to a workflow
        if (state.workflowId != null) {
            val tciw = JobCompletedInWorkflow(
                workflowId = state.workflowId,
                jobId = state.jobId,
                jobOutput = msg.jobOutput
            )
            workflowDispatcher.dispatch(tciw)
        }

        // log event
        val tc = JobCompleted(
            jobId = state.jobId,
            jobOutput = msg.jobOutput
        )
        dispatcher.toEngine(tc)

        // Delete stored state
        storage.deleteState(state.jobId)
    }

    private fun taskAttemptFailed(state: EngineState, msg: JobAttemptFailed) {
        state.jobStatus = JobStatus.RUNNING_ERROR

        delayRetryJobAttempt(state = state, delay = msg.jobAttemptDelayBeforeRetry)
    }

    private fun delayRetryJobAttempt(state: EngineState, delay: Float?) {
        // no retry
        if (delay == null) return
        // immediate retry
        if (delay <= 0f) return retryJobAttempt(state)
        // delayed retry
        if (delay > 0f) {
            state.jobStatus = JobStatus.RUNNING_WARNING

            // schedule next attempt
            val tar = RetryJobAttempt(
                jobId = state.jobId,
                jobAttemptId = state.jobAttemptId,
                jobAttemptRetry = state.jobAttemptRetry,
                jobAttemptIndex = state.jobAttemptIndex
            )
            dispatcher.toEngine(tar, after = delay)
        }
    }
}
