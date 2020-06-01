package com.zenaton.taskManager.engine

import com.zenaton.taskManager.data.JobAttemptId
import com.zenaton.taskManager.data.JobStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import com.zenaton.taskManager.messages.JobAttemptMessage
import com.zenaton.taskManager.monitoring.perName.JobStatusUpdated
import com.zenaton.taskManager.workers.RunJob
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as TaskCompletedInWorkflow

class Engine {
    lateinit var taskDispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcherInterface
    lateinit var storage: EngineStorage
    lateinit var logger: Logger

    fun handle(msg: EngineMessage) {
        // get associated state
        val oldState = storage.getState(msg.jobId)
        var newState = oldState?.copy()

        if (newState == null) {
            // a null state should mean that this task is already terminated => all messages others than TaskDispatched are ignored
            if (msg !is DispatchJob) {
                logger.warn("No state found for message: (It's normal if this task is already terminated)%s", msg, null)
                return
            }
            // init a state
            newState = EngineState(
                jobId = msg.jobId,
                jobName = msg.jobName,
                jobData = msg.jobData,
                workflowId = msg.workflowId,
                jobAttemptId = JobAttemptId(),
                jobAttemptIndex = 0,
                jobStatus = JobStatus.RUNNING_OK
            )
        } else {
            // this should never happen
            if (newState.jobId != msg.jobId) {
                logger.error("Inconsistent taskId in message:%s and State:%s)", msg, newState)
                return
            }
            // a non-null state with TaskDispatched should mean that this message has been replicated
            if (msg is DispatchJob) {
                logger.error("Already existing state:%s for message:%s", msg, newState)
                return
            }
            // check taskAttemptId and taskAttemptIndex consistency
            if (msg is JobAttemptMessage && msg !is JobAttemptCompleted) {
                if (newState.jobAttemptId != msg.jobAttemptId) {
                    logger.warn("Inconsistent jobAttemptId in message: (Can happen if the job has been manually retried)%s and State:%s", msg, newState)
                    return
                }
                if (newState.jobAttemptIndex != msg.jobAttemptIndex) {
                    logger.warn("Inconsistent jobAttemptIndex in message: (Can happen if this job has had timeout)%s and State:%s", msg, newState)
                    return
                }
            }
        }

        when (msg) {
            is CancelJob -> cancelTask(newState, msg)
            is DispatchJob -> dispatchTask(newState, msg)
            is RetryJob -> retryTask(newState, msg)
            is RetryJobAttempt -> retryTaskAttempt(newState, msg)
            is JobAttemptCompleted -> taskAttemptCompleted(newState, msg)
            is JobAttemptDispatched -> Unit
            is JobAttemptFailed -> taskAttemptFailed(newState, msg)
            is JobAttemptStarted -> Unit
            is JobCanceled -> Unit
            is JobCompleted -> Unit
            is JobDispatched -> Unit
        }

        // Update stored state if needed and existing
        if (newState != oldState && !newState.jobStatus.isTerminated) {
            storage.updateState(msg.jobId, newState, oldState)
        }

        // Send TaskStatusUpdated if needed
        if (oldState?.jobStatus != newState.jobStatus) {
            val tsc = JobStatusUpdated(
                jobId = newState.jobId,
                jobName = newState.jobName,
                oldStatus = oldState?.jobStatus,
                newStatus = newState.jobStatus
            )

            taskDispatcher.dispatch(tsc)
        }
    }

    private fun cancelTask(state: EngineState, msg: CancelJob) {
        state.jobStatus = JobStatus.TERMINATED_CANCELED

        // log event
        val tad = JobCanceled(
            jobId = msg.jobId
        )
        taskDispatcher.dispatch(tad)

        // Delete stored state
        storage.deleteState(state.jobId)
    }

    private fun dispatchTask(state: EngineState, msg: DispatchJob) {
        state.jobStatus = JobStatus.RUNNING_OK

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        taskDispatcher.dispatch(rt)

        // log events
        val td = JobDispatched(
            jobId = state.jobId
        )
        taskDispatcher.dispatch(td)

        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex
        )
        taskDispatcher.dispatch(tad)
    }

    private fun retryTask(state: EngineState, msg: RetryJob) {
        state.jobStatus = JobStatus.RUNNING_WARNING
        state.jobAttemptId = JobAttemptId()
        state.jobAttemptIndex = 0

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tad = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex
        )
        taskDispatcher.dispatch(tad)
    }

    private fun retryTaskAttempt(state: EngineState, msg: EngineMessage) {
        state.jobStatus = JobStatus.RUNNING_WARNING
        state.jobAttemptIndex = state.jobAttemptIndex + 1

        // send task to workers
        val rt = RunJob(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex,
            jobName = state.jobName,
            jobData = state.jobData
        )
        taskDispatcher.dispatch(rt)

        // log event
        val tar = JobAttemptDispatched(
            jobId = state.jobId,
            jobAttemptId = state.jobAttemptId,
            jobAttemptIndex = state.jobAttemptIndex
        )
        taskDispatcher.dispatch(tar)
    }

    private fun taskAttemptCompleted(state: EngineState, msg: JobAttemptCompleted) {
        state.jobStatus = JobStatus.TERMINATED_COMPLETED

        // if this task belongs to a workflow
        if (state.workflowId != null) {
            val tciw = TaskCompletedInWorkflow(
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
        taskDispatcher.dispatch(tc)

        // Delete stored state
        storage.deleteState(state.jobId)
    }

    private fun taskAttemptFailed(state: EngineState, msg: JobAttemptFailed) {
        state.jobStatus = JobStatus.RUNNING_ERROR

        delayRetryTaskAttempt(state = state, msg = msg, delay = msg.jobAttemptDelayBeforeRetry)
    }

    private fun delayRetryTaskAttempt(state: EngineState, msg: EngineMessage, delay: Float?) {
        // no retry
        if (delay == null) return
        // immediate retry
        if (delay <= 0f) return retryTaskAttempt(state, msg)
        // delayed retry
        if (delay > 0f) {
            state.jobStatus = JobStatus.RUNNING_WARNING

            // schedule next attempt
            val tar = RetryJobAttempt(
                jobId = state.jobId,
                jobAttemptId = state.jobAttemptId,
                jobAttemptIndex = state.jobAttemptIndex
            )
            taskDispatcher.dispatch(tar, after = delay)
        }
    }
}
