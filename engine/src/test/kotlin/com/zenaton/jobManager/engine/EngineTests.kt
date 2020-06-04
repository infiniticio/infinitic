package com.zenaton.jobManager.engine

import com.zenaton.commons.data.interfaces.deepCopy
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.logger.Logger
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
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.messages.interfaces.JobAttemptMessage
import com.zenaton.jobManager.utils.TestFactory
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
import io.mockk.verifyAll
import io.mockk.verifyOrder

fun state(values: Map<String, Any?>? = null) = TestFactory.get(EngineState::class, values)

fun cancelJob(values: Map<String, Any?>? = null) = TestFactory.get(CancelJob::class, values)
fun dispatchJob(values: Map<String, Any?>? = null) = TestFactory.get(DispatchJob::class, values)
fun retryJob(values: Map<String, Any?>? = null) = TestFactory.get(RetryJob::class, values)
fun retryJobAttempt(values: Map<String, Any?>? = null) = TestFactory.get(RetryJobAttempt::class, values)
fun workerMessage(values: Map<String, Any?>? = null) = TestFactory.get(ForWorkerMessage::class, values)

fun jobAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptCompleted::class, values)
fun jobAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptDispatched::class, values)
fun jobAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptFailed::class, values)
fun jobAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.get(JobAttemptStarted::class, values)
fun jobCanceled(values: Map<String, Any?>? = null) = TestFactory.get(JobCanceled::class, values)
fun jobCompleted(values: Map<String, Any?>? = null) = TestFactory.get(JobCompleted::class, values)

class EngineResults {
    lateinit var jobDispatcher: Dispatcher
    lateinit var workflowDispatcher: WorkflowDispatcher
    lateinit var storage: EngineStorage
    lateinit var logger: Logger
    var state: EngineState? = null
    var workerMessage: ForWorkerMessage? = null
    var retryJobAttempt: RetryJobAttempt? = null
    var retryJobAttemptDelay: Float? = null
    var jobAttemptCompleted: JobAttemptCompleted? = null
    var jobAttemptDispatched: JobAttemptDispatched? = null
    var jobAttemptFailed: JobAttemptFailed? = null
    var jobAttemptStarted: JobAttemptStarted? = null
    var jobCanceled: JobCanceled? = null
    var jobCompleted: JobCompleted? = null
    var jobCompletedInWorkflow: JobCompletedInWorkflow? = null
    var jobStatusUpdated: JobStatusUpdated? = null
}

fun engineHandle(stateIn: EngineState?, msgIn: ForEngineMessage): EngineResults {
    // deep copy of stateIn to avoid updating it
    val state: EngineState? = stateIn?.let { it.deepCopy() }
    // mocking
    val jobDispatcher = mockk<Dispatcher>()
    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val stateStorage = mockk<EngineStorage>()
    val logger = mockk<Logger>()
    val stateSlot = slot<EngineState>()
    val jobAttemptCompletedSlot = slot<JobAttemptCompleted>()
    val jobAttemptDispatchedSlot = slot<JobAttemptDispatched>()
    val jobAttemptFailedSlot = slot<JobAttemptFailed>()
    val jobAttemptStartedSlot = slot<JobAttemptStarted>()
    val jobCanceledSlot = slot<JobCanceled>()
    val jobCompletedSlot = slot<JobCompleted>()
    val retryJobAttemptSlot = slot<RetryJobAttempt>()
    val retryJobAttemptDelaySlot = slot<Float>()
    val workerMessageSlot = slot<ForWorkerMessage>()
    val jobCompletedInWorkflowSlot = slot<JobCompletedInWorkflow>()
    val jobStatusUpdatedSlot = slot<JobStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } returns "error!"
    every { logger.warn(any(), msgIn, stateIn) } returns "warn!"
    every { stateStorage.getState(msgIn.jobId) } returns state
    every { stateStorage.updateState(any(), capture(stateSlot), any()) } just Runs
    every { stateStorage.deleteState(any()) } just Runs
    every { jobDispatcher.toWorkers(capture(workerMessageSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(retryJobAttemptSlot), capture(retryJobAttemptDelaySlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobAttemptCompletedSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobAttemptDispatchedSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobAttemptFailedSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobAttemptStartedSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobCanceledSlot)) } just Runs
    every { jobDispatcher.toEngine(capture(jobCompletedSlot)) } just Runs
    every { jobDispatcher.toMonitoringPerName(capture(jobStatusUpdatedSlot)) } just Runs
    every { workflowDispatcher.dispatch(capture(jobCompletedInWorkflowSlot)) } just Runs
    // given
    val engine = Engine()
    engine.dispatch = jobDispatcher
    engine.storage = stateStorage
    engine.workflowDispatcher = workflowDispatcher
    engine.logger = logger
    // when
    engine.handle(message = msgIn)
    // then
    val o = EngineResults()
    o.jobDispatcher = jobDispatcher
    o.workflowDispatcher = workflowDispatcher
    o.storage = stateStorage
    o.logger = logger
    if (stateSlot.isCaptured) o.state = stateSlot.captured
    if (workerMessageSlot.isCaptured) o.workerMessage = workerMessageSlot.captured
    if (retryJobAttemptSlot.isCaptured) o.retryJobAttempt = retryJobAttemptSlot.captured
    if (retryJobAttemptDelaySlot.isCaptured) o.retryJobAttemptDelay = retryJobAttemptDelaySlot.captured
    if (jobAttemptCompletedSlot.isCaptured) o.jobAttemptCompleted = jobAttemptCompletedSlot.captured
    if (jobAttemptDispatchedSlot.isCaptured) o.jobAttemptDispatched = jobAttemptDispatchedSlot.captured
    if (jobAttemptFailedSlot.isCaptured) o.jobAttemptFailed = jobAttemptFailedSlot.captured
    if (jobAttemptStartedSlot.isCaptured) o.jobAttemptStarted = jobAttemptStartedSlot.captured
    if (jobCanceledSlot.isCaptured) o.jobCanceled = jobCanceledSlot.captured
    if (jobCompletedSlot.isCaptured) o.jobCompleted = jobCompletedSlot.captured
    if (jobStatusUpdatedSlot.isCaptured) o.jobStatusUpdated = jobStatusUpdatedSlot.captured
    if (jobCompletedInWorkflowSlot.isCaptured) o.jobCompletedInWorkflow = jobCompletedInWorkflowSlot.captured

    return o
}

class EngineTests : StringSpec({
    // Note: dispatchJob is voluntary excluded of this test
    include(shouldWarnIfNotState(cancelJob()))
    include(shouldWarnIfNotState(retryJob()))
    include(shouldWarnIfNotState(retryJobAttempt()))
    include(shouldWarnIfNotState(jobAttemptCompleted()))
    include(shouldWarnIfNotState(jobAttemptFailed()))
    include(shouldWarnIfNotState(jobAttemptStarted()))

    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(cancelJob()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(retryJob()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(retryJobAttempt()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(jobAttemptCompleted()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(jobAttemptFailed()))
    include(shouldErrorIfStateAndMessageHaveInconsistentJobId(jobAttemptStarted()))

    // Note: jobAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(retryJobAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(jobAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(jobAttemptStarted()))

    // Note: jobAttemptCompleted is voluntary excluded of this test
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(retryJobAttempt()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(jobAttemptFailed()))
    include(shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(jobAttemptStarted()))

    "Should error if job dispatched with existing state" {
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
            o.jobDispatcher.toEngine(o.jobCanceled!!)
            o.storage.deleteState(msgIn.jobId)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.jobCanceled!!.jobId shouldBe msgIn.jobId
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_CANCELED
    }

    "Dispatch Job" {
        val msgIn = dispatchJob()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.jobDispatcher.toWorkers(o.workerMessage!!)
            o.jobDispatcher.toEngine(o.jobAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, null)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunJob) shouldBe true
        val run = o.workerMessage as RunJob
        run.jobId shouldBe msgIn.jobId
        run.jobName shouldBe msgIn.jobName
        run.jobData shouldBe msgIn.jobData
        run.jobAttemptRetry.int shouldBe 0
        run.jobAttemptId shouldBe o.jobAttemptDispatched!!.jobAttemptId
        run.jobAttemptRetry shouldBe o.jobAttemptDispatched!!.jobAttemptRetry
        o.state!!.jobId shouldBe msgIn.jobId
        o.state!!.jobName shouldBe msgIn.jobName
        o.state!!.jobData shouldBe msgIn.jobData
        o.state!!.jobAttemptId shouldBe run.jobAttemptId
        o.state!!.jobAttemptRetry.int shouldBe 0
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
        verifyAll {
            o.storage.getState(msgIn.jobId)
            o.jobDispatcher.toWorkers(o.workerMessage!!)
            o.jobDispatcher.toEngine(o.jobAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunJob) shouldBe true
        val run = o.workerMessage as RunJob
        run.jobId shouldBe stateIn.jobId
        run.jobAttemptId shouldNotBe stateIn.jobAttemptId
        run.jobAttemptRetry.int shouldBe 0
        run.jobName shouldBe stateIn.jobName
        run.jobData shouldBe stateIn.jobData
        o.jobAttemptDispatched!!.jobId shouldBe stateIn.jobId
        o.jobAttemptDispatched!!.jobAttemptId shouldBe run.jobAttemptId
        o.jobAttemptDispatched!!.jobAttemptRetry.int shouldBe 0
        o.state!!.jobId shouldBe stateIn.jobId
        o.state!!.jobName shouldBe stateIn.jobName
        o.state!!.jobData shouldBe stateIn.jobData
        o.state!!.jobAttemptId shouldBe run.jobAttemptId
        o.state!!.jobAttemptRetry shouldBe run.jobAttemptRetry
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
            "jobAttemptRetry" to stateIn.jobAttemptRetry
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Completed" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptCompleted(mapOf(
            "jobId" to stateIn.jobId,
            "workflowId" to TestFactory.get(WorkflowId::class)
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.workflowDispatcher.dispatch(o.jobCompletedInWorkflow!!)
            o.jobDispatcher.toEngine(o.jobCompleted!!)
            o.storage.deleteState(msgIn.jobId)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.jobCompletedInWorkflow!!.jobId shouldBe stateIn.jobId
        o.jobCompletedInWorkflow!!.workflowId shouldBe stateIn.workflowId
        o.jobCompletedInWorkflow!!.jobOutput shouldBe msgIn.jobOutput
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_COMPLETED
    }

    "Job Attempt Failed without retry" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry,
            "jobAttemptDelayBeforeRetry" to null
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "Job Attempt Failed with future retry" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry,
            "jobAttemptDelayBeforeRetry" to 42F
        ))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.jobDispatcher.toEngine(o.retryJobAttempt!!, o.retryJobAttemptDelay!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.retryJobAttempt!!.jobId shouldBe stateIn.jobId
        o.retryJobAttempt!!.jobAttemptId shouldBe stateIn.jobAttemptId
        o.retryJobAttempt!!.jobAttemptRetry shouldBe stateIn.jobAttemptRetry
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
        val msgIn = jobAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry,
            "jobAttemptDelayBeforeRetry" to 0F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Failed with immediate retry (negative delay)" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptFailed(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry,
            "jobAttemptDelayBeforeRetry" to -42F
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "Job Attempt Started" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptStarted(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }

    "Job attempt started on a OK job should do nothing" {
        val stateIn = state(mapOf(
            "jobStatus" to JobStatus.RUNNING_OK
        ))
        val msgIn = jobAttemptStarted(mapOf(
            "jobId" to stateIn.jobId,
            "jobAttemptId" to stateIn.jobAttemptId,
            "jobAttemptRetry" to stateIn.jobAttemptRetry
        ))
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(msgIn, o)
    }
})

private fun shouldWarnIfNotState(msgIn: ForEngineMessage) = stringSpec {
    val o = engineHandle(null, msgIn)
    checkShouldWarnAndDoNothingMore(null, msgIn, o)
}

private fun shouldErrorIfStateAndMessageHaveInconsistentJobId(msgIn: ForEngineMessage) = stringSpec {
    val stateIn = state()
    val o = engineHandle(stateIn, msgIn)
    checkShouldErrorAndDoNothingMore(stateIn, msgIn, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptId(msgIn: JobAttemptMessage) = stringSpec {
    val stateIn = state(mapOf("jobId" to msgIn.jobId))
    val o = engineHandle(stateIn, msgIn as ForEngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as ForEngineMessage, o)
}

private fun shouldWarnIfStateAndAttemptMessageHaveInconsistentAttemptIndex(msgIn: JobAttemptMessage) = stringSpec {
    val stateIn = state(mapOf(
        "jobId" to msgIn.jobId,
        "jobAttemptId" to msgIn.jobAttemptId
    ))
    val o = engineHandle(stateIn, msgIn as ForEngineMessage)
    checkShouldWarnAndDoNothingMore(stateIn, msgIn as ForEngineMessage, o)
}

private fun checkShouldDoNothing(msgIn: ForEngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
    }
    checkConfirmVerified(o)
}

private fun checkShouldRetryJobAttempt(msgIn: ForEngineMessage, stateIn: EngineState, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.jobDispatcher.toWorkers(o.workerMessage!!)
        o.jobDispatcher.toEngine(o.jobAttemptDispatched!!)
        o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
        o.jobDispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
    }
    checkConfirmVerified(o)
    (o.workerMessage is RunJob) shouldBe true
    val run = o.workerMessage as RunJob
    run.jobId shouldBe stateIn.jobId
    run.jobAttemptId shouldBe stateIn.jobAttemptId
    run.jobAttemptRetry shouldBe stateIn.jobAttemptRetry + 1
    run.jobName shouldBe stateIn.jobName
    run.jobData shouldBe stateIn.jobData
    o.jobAttemptDispatched!!.jobId shouldBe stateIn.jobId
    o.jobAttemptDispatched!!.jobAttemptId shouldBe run.jobAttemptId
    o.jobAttemptDispatched!!.jobAttemptRetry shouldBe run.jobAttemptRetry
    o.state!!.jobId shouldBe stateIn.jobId
    o.state!!.jobName shouldBe stateIn.jobName
    o.state!!.jobData shouldBe stateIn.jobData
    o.state!!.jobAttemptId shouldBe run.jobAttemptId
    o.state!!.jobAttemptRetry shouldBe run.jobAttemptRetry
    o.state!!.jobStatus shouldBe JobStatus.RUNNING_WARNING
    o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
    o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
}

private fun checkShouldWarnAndDoNothingMore(stateIn: EngineState?, msgIn: ForEngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.logger.warn(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkShouldErrorAndDoNothingMore(stateIn: EngineState?, msgIn: ForEngineMessage, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.logger.error(any(), msgIn, stateIn)
    }
    checkConfirmVerified(o)
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.jobDispatcher)
    confirmVerified(o.workflowDispatcher)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}
