package com.zenaton.taskManager.engine

import com.zenaton.commons.utils.TestFactory
import com.zenaton.taskManager.data.JobStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import com.zenaton.taskManager.messages.JobAttemptMessage
import com.zenaton.taskManager.monitoring.perName.JobStatusUpdated
import com.zenaton.taskManager.workers.RunJob
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted as JobCompletedInWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder

fun state(values: Map<String, Any?>? = null) = TestFactory.get(EngineState::class, values)

fun cancelJob(values: Map<String, Any?>? = null) = TestFactory.get(CancelJob::class, values)
fun dispatchJob(values: Map<String, Any?>? = null) = TestFactory.get(DispatchJob::class, values)
fun retryJob(values: Map<String, Any?>? = null) = TestFactory.get(RetryJob::class, values)
fun retryJobAttempt(values: Map<String, Any?>? = null) = TestFactory.get(RetryJobAttempt::class, values)
fun runJob(values: Map<String, Any?>? = null) = TestFactory.get(RunJob::class, values)

fun taskAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptCompleted::class, values)
fun taskAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptDispatched::class, values)
fun taskAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptFailed::class, values)
fun taskAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptStarted::class, values)
fun taskCanceled(values: Map<String, Any?>? = null) = TestFactory.get(JobCanceled::class, values)
fun taskCompleted(values: Map<String, Any?>? = null) = TestFactory.get(JobCompleted::class, values)
fun taskDispatched(values: Map<String, Any?>? = null) = TestFactory.get(JobDispatched::class, values)

class EngineResults {
    lateinit var taskDispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var storage: EngineStorage
    lateinit var logger: Logger
    var state: EngineState? = null
    var runJob: RunJob? = null
    var retryJobAttempt: RetryJobAttempt? = null
    var retryJobAttemptDelay: Float? = null
    var taskAttemptCompleted: JobAttemptCompleted? = null
    var taskAttemptDispatched: JobAttemptDispatched? = null
    var taskAttemptFailed: JobAttemptFailed? = null
    var taskAttemptStarted: JobAttemptStarted? = null
    var taskCanceled: JobCanceled? = null
    var taskCompleted: JobCompleted? = null
    var taskDispatched: JobDispatched? = null
    var taskCompletedInWorkflow: JobCompletedInWorkflow? = null
    var jobStatusUpdated: JobStatusUpdated? = null
}

fun engineHandle(stateIn: EngineState?, msgIn: EngineMessage): EngineResults {
    // avoid deep updates of stateIn
    val state = stateIn?.copy()
    // mocking
    val taskDispatcher = mockk<Dispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stateStorage = mockk<EngineStorage>()
    val logger = mockk<Logger>()
    val stateSlot = slot<EngineState>()
    val taskAttemptCompletedSlot = slot<JobAttemptCompleted>()
    val taskAttemptDispatchedSlot = slot<JobAttemptDispatched>()
    val taskAttemptFailedSlot = slot<JobAttemptFailed>()
    val taskAttemptStartedSlot = slot<JobAttemptStarted>()
    val taskCanceledSlot = slot<JobCanceled>()
    val taskCompletedSlot = slot<JobCompleted>()
    val taskDispatchedSlot = slot<JobDispatched>()
    val retryJobAttemptSlot = slot<RetryJobAttempt>()
    val retryJobAttemptDelaySlot = slot<Float>()
    val runJobSlot = slot<RunJob>()
    val taskCompletedInWorkflowSlot = slot<JobCompletedInWorkflow>()
    val jobStatusUpdatedSlot = slot<JobStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } returns "error!"
    every { logger.warn(any(), msgIn, stateIn) } returns "warn!"
    every { stateStorage.getState(msgIn.jobId) } returns state
    every { stateStorage.updateState(any(), capture(stateSlot), any()) } just Runs
    every { stateStorage.deleteState(any()) } just Runs
    every { taskDispatcher.dispatch(capture(runJobSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(retryJobAttemptSlot), capture(retryJobAttemptDelaySlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptCompletedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptDispatchedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptFailedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskAttemptStartedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskCanceledSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskCompletedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(taskDispatchedSlot)) } just Runs
    every { taskDispatcher.dispatch(capture(jobStatusUpdatedSlot)) } just Runs
    every { workflowDispatcher.dispatch(capture(taskCompletedInWorkflowSlot)) } just Runs
    // given
    val engine = Engine()
    engine.taskDispatcher = taskDispatcher
    engine.storage = stateStorage
    engine.workflowDispatcher = workflowDispatcher
    engine.logger = logger
    // when
    engine.handle(msg = msgIn)
    // then
    val o = EngineResults()
    o.taskDispatcher = taskDispatcher
    o.workflowDispatcher = workflowDispatcher
    o.storage = stateStorage
    o.logger = logger
    if (stateSlot.isCaptured) o.state = stateSlot.captured
    if (runJobSlot.isCaptured) o.runJob = runJobSlot.captured
    if (retryJobAttemptSlot.isCaptured) o.retryJobAttempt = retryJobAttemptSlot.captured
    if (retryJobAttemptDelaySlot.isCaptured) o.retryJobAttemptDelay = retryJobAttemptDelaySlot.captured
    if (taskAttemptCompletedSlot.isCaptured) o.taskAttemptCompleted = taskAttemptCompletedSlot.captured
    if (taskAttemptDispatchedSlot.isCaptured) o.taskAttemptDispatched = taskAttemptDispatchedSlot.captured
    if (taskAttemptFailedSlot.isCaptured) o.taskAttemptFailed = taskAttemptFailedSlot.captured
    if (taskAttemptStartedSlot.isCaptured) o.taskAttemptStarted = taskAttemptStartedSlot.captured
    if (taskCanceledSlot.isCaptured) o.taskCanceled = taskCanceledSlot.captured
    if (taskCompletedSlot.isCaptured) o.taskCompleted = taskCompletedSlot.captured
    if (taskDispatchedSlot.isCaptured) o.taskDispatched = taskDispatchedSlot.captured
    if (jobStatusUpdatedSlot.isCaptured) o.jobStatusUpdated = jobStatusUpdatedSlot.captured
    if (taskCompletedInWorkflowSlot.isCaptured) o.taskCompletedInWorkflow = taskCompletedInWorkflowSlot.captured

    return o
}

class EngineTests : StringSpec({
    // Note: dispatchJob is voluntary excluded of this test
    include(shouldWarnIfNotState(cancelJob()))
    include(shouldWarnIfNotState(retryJob()))
    include(shouldWarnIfNotState(retryJobAttempt()))
    include(shouldWarnIfNotState(taskAttemptCompleted()))
    include(shouldWarnIfNotState(taskAttemptDispatched()))
    include(shouldWarnIfNotState(taskAttemptFailed()))
    include(shouldWarnIfNotState(taskAttemptStarted()))
    include(shouldWarnIfNotState(taskCanceled()))
    include(shouldWarnIfNotState(taskCompleted()))
    include(shouldWarnIfNotState(taskDispatched()))

    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(cancelJob()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(retryJob()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(retryJobAttempt()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskAttemptCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskAttemptDispatched()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskAttemptFailed()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskAttemptStarted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskCanceled()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(taskDispatched()))

    // Note: taskAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(retryJobAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(taskAttemptStarted()))

    // Note: taskAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(retryJobAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptDispatched()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(taskAttemptStarted()))

    "Should error if task dispatched with existing state" {
        val stateIn = state()
        val msgIn = dispatchJob(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.logger.error(any(), msgIn, stateIn)
        }
        checkConfirmVerified(o)
    }

    "Cancel Job" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = cancelJob(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.taskCanceled!!)
            o.storage.deleteState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCanceled!!.jobId shouldBe msgIn.jobId
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_CANCELED
    }

    "Dispatch Job" {
        val msgIn = dispatchJob()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.runJob!!)
            o.taskDispatcher.dispatch(o.taskDispatched!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, null)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.runJob!!.jobId shouldBe msgIn.jobId
        o.runJob!!.jobName shouldBe msgIn.jobName
        o.runJob!!.jobData shouldBe msgIn.jobData
        o.runJob!!.jobAttemptIndex shouldBe 0
        o.runJob!!.jobAttemptId shouldBe o.taskAttemptDispatched!!.jobAttemptId
        o.runJob!!.jobAttemptIndex shouldBe o.taskAttemptDispatched!!.jobAttemptIndex
        o.state!!.jobId shouldBe msgIn.jobId
        o.state!!.jobName shouldBe msgIn.jobName
        o.state!!.jobData shouldBe msgIn.jobData
        o.state!!.jobAttemptId shouldBe o.runJob!!.jobAttemptId
        o.state!!.jobAttemptIndex shouldBe 0
        o.state!!.workflowId shouldBe msgIn.workflowId
        o.state!!.jobStatus shouldBe JobStatus.RUNNING_OK
        o.jobStatusUpdated!!.oldStatus shouldBe null
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_OK
    }

    "Retry Job" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_ERROR
        ))
        val msgIn = retryJob(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.runJob!!)
            o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.runJob!!.jobId shouldBe stateIn.jobId
        o.runJob!!.jobAttemptId shouldNotBe stateIn.jobAttemptId
        o.runJob!!.jobAttemptIndex shouldBe 0
        o.runJob!!.jobName shouldBe stateIn.jobName
        o.runJob!!.jobData shouldBe stateIn.jobData
        o.taskAttemptDispatched!!.jobId shouldBe stateIn.jobId
        o.taskAttemptDispatched!!.jobAttemptId shouldBe o.runJob!!.jobAttemptId
        o.taskAttemptDispatched!!.jobAttemptIndex shouldBe 0
        o.state!!.jobId shouldBe stateIn.jobId
        o.state!!.jobName shouldBe stateIn.jobName
        o.state!!.jobData shouldBe stateIn.jobData
        o.state!!.jobAttemptId shouldBe o.runJob!!.jobAttemptId
        o.state!!.jobAttemptIndex shouldBe o.runJob!!.jobAttemptIndex
        o.state!!.jobStatus shouldBe JobStatus.RUNNING_WARNING
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
    }

    "Retry Job Attempt" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_ERROR
        ))
        val msgIn = retryJobAttempt(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Completed" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptCompleted(mapOf(
            "jobId" to stateIn.jobId,
            "workflowId" to TestFactory.get(WorkflowId::class)
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.workflowDispatcher.dispatch(o.taskCompletedInWorkflow!!)
            o.taskDispatcher.dispatch(o.taskCompleted!!)
            o.storage.deleteState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.taskCompletedInWorkflow!!.jobId shouldBe stateIn.jobId
        o.taskCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
        o.taskCompletedInWorkflow!!.jobOutput shouldBe msgIn.jobOutput
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_COMPLETED
    }

    "Job Attempt Failed without retry" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex,
            "jobAttemptDelayBeforeRetry" to null
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "Job Attempt Failed with future retry" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex,
            "jobAttemptDelayBeforeRetry" to 42F
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.taskDispatcher.dispatch(o.retryJobAttempt!!, o.retryJobAttemptDelay!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.retryJobAttempt!!.jobId shouldBe stateIn.jobId
        o.retryJobAttempt!!.jobAttemptId shouldBe stateIn.jobAttemptId
        o.retryJobAttempt!!.jobAttemptIndex shouldBe stateIn.jobAttemptIndex
        o.retryJobAttemptDelay!! shouldBe msgIn.jobAttemptDelayBeforeRetry
        o.jobStatusUpdated!!.jobId shouldBe stateIn.jobId
        o.jobStatusUpdated!!.jobName shouldBe stateIn.jobName
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
    }

    "Job Attempt Failed with immediate retry" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex,
            "jobAttemptDelayBeforeRetry" to 0F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Failed with immediate retry (negative delay)" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex,
            "jobAttemptDelayBeforeRetry" to -42F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Started" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptStarted(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Job attempt started on a OK task should do nothing" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = taskAttemptStarted(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptIndex" to stateIn.jobAttemptIndex
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Job Canceled" {
        val stateIn = state()
        val msgIn = taskCanceled(mapOf(
            "jobId" to stateIn.jobId
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
        }
        checkConfirmVerified(o)
    }

    "Job Completed" {
        val stateIn = state()
        val msgIn = taskCompleted(mapOf(
            "jobId" to stateIn.jobId
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
        }
        checkConfirmVerified(o)
    }

    "Job Dispatched" {
        val stateIn = state()
        val msgIn = taskDispatched(mapOf(
            "jobId" to stateIn.jobId
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }
})

private fun shouldWarnIfNotState(msgIn: EngineMessage) = stringSpec {
    val o = engineHandle(null, msgIn)
    checkShouldWarnAndDoNothingMore(null, msgIn, o)
}

private fun shouldErrorIfStateAndMessageHaveInconsistentJobId(msgIn: EngineMessage) = stringSpec {
    val stateIn = state()
    val o = engineHandle(stateIn, msgIn)
    checkShouldErrorAndDoNothingMore(stateIn, msgIn, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(msgIn: JobAttemptMessage) = stringSpec {
    val stateIn = state(mapOf("jobId" to msgIn.jobId))
    val o = engineHandle(stateIn, msgIn as EngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as EngineMessage, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(msgIn: JobAttemptMessage) = stringSpec {
    val stateIn = state(mapOf(
        "jobId" to msgIn.jobId,
        "jobAttemptId" to msgIn.jobAttemptId
    ))
    val o = engineHandle(stateIn, msgIn as EngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as EngineMessage, o)
}

private fun checkShouldDoNothing(msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
    }
    checkConfirmVerified(o)
}

private fun checkShouldRetryJobAttempt(msgIn: EngineMessage, stateIn: EngineState, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.taskDispatcher.dispatch(o.runJob!!)
        o.taskDispatcher.dispatch(o.taskAttemptDispatched!!)
        o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
        o.taskDispatcher.dispatch(o.jobStatusUpdated!!)
    }
    checkConfirmVerified(o)
    o.runJob!!.jobId shouldBe stateIn.jobId
    o.runJob!!.jobAttemptId shouldBe stateIn.jobAttemptId
    o.runJob!!.jobAttemptIndex shouldBe stateIn.jobAttemptIndex + 1
    o.runJob!!.jobName shouldBe stateIn.jobName
    o.runJob!!.jobData shouldBe stateIn.jobData
    o.taskAttemptDispatched!!.jobId shouldBe stateIn.jobId
    o.taskAttemptDispatched!!.jobAttemptId shouldBe o.runJob!!.jobAttemptId
    o.taskAttemptDispatched!!.jobAttemptIndex shouldBe o.runJob!!.jobAttemptIndex
    o.state!!.jobId shouldBe stateIn.jobId
    o.state!!.jobName shouldBe stateIn.jobName
    o.state!!.jobData shouldBe stateIn.jobData
    o.state!!.jobAttemptId shouldBe o.runJob!!.jobAttemptId
    o.state!!.jobAttemptIndex shouldBe o.runJob!!.jobAttemptIndex
    o.state!!.jobStatus shouldBe JobStatus.RUNNING_WARNING
    o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
    o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
}

private fun checkShouldWarnAndDoNothingMore(stateIn: EngineState?, msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.logger.warn(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkShouldErrorAndDoNothingMore(stateIn: EngineState?, msgIn: EngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.logger.error(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.taskDispatcher)
    confirmVerified(o.workflowDispatcher)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}
