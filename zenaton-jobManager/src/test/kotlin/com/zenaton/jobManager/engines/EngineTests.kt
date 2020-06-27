package com.zenaton.jobManager.engines

import com.zenaton.common.data.interfaces.plus
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.ForJobEngineMessage
import com.zenaton.jobManager.messages.ForWorkerMessage
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
import com.zenaton.jobManager.states.JobEngineState
import com.zenaton.jobManager.storages.JobEngineStateStorage
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
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
import org.slf4j.Logger

internal class EngineResults {
    lateinit var dispatcher: Dispatcher
    lateinit var storage: JobEngineStateStorage
    lateinit var logger: Logger
    var state: JobEngineState? = null
    var workerMessage: ForWorkerMessage? = null
    var retryJobAttempt: RetryJobAttempt? = null
    var retryJobAttemptDelay: Float? = null
    var jobAttemptCompleted: JobAttemptCompleted? = null
    var jobAttemptDispatched: JobAttemptDispatched? = null
    var jobAttemptFailed: JobAttemptFailed? = null
    var jobAttemptStarted: JobAttemptStarted? = null
    var jobCanceled: JobCanceled? = null
    var jobCompleted: JobCompleted? = null
    var jobStatusUpdated: JobStatusUpdated? = null
}

internal fun engineHandle(stateIn: JobEngineState?, msgIn: ForJobEngineMessage): EngineResults {
    // deep copy of stateIn to avoid updating it
    val state: JobEngineState? = stateIn?.deepCopy()
    // mocking
    val logger = mockk<Logger>()
    val storage = mockk<JobEngineStateStorage>()
    val dispatcher = mockk<Dispatcher>()
    val stateSlot = slot<JobEngineState>()
    val jobAttemptCompletedSlot = slot<JobAttemptCompleted>()
    val jobAttemptDispatchedSlot = slot<JobAttemptDispatched>()
    val jobAttemptFailedSlot = slot<JobAttemptFailed>()
    val jobAttemptStartedSlot = slot<JobAttemptStarted>()
    val jobCanceledSlot = slot<JobCanceled>()
    val jobCompletedSlot = slot<JobCompleted>()
    val retryJobAttemptSlot = slot<RetryJobAttempt>()
    val retryJobAttemptDelaySlot = slot<Float>()
    val workerMessageSlot = slot<ForWorkerMessage>()
    val jobStatusUpdatedSlot = slot<JobStatusUpdated>()
    every { logger.error(any(), msgIn, stateIn) } just Runs
    every { logger.warn(any(), msgIn, stateIn) } just Runs
    every { storage.getState(any()) } returns state
    every { storage.updateState(any(), capture(stateSlot), any()) } just Runs
    every { storage.deleteState(any()) } just Runs
    every { dispatcher.toWorkers(capture(workerMessageSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(retryJobAttemptSlot), capture(retryJobAttemptDelaySlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobAttemptCompletedSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobAttemptDispatchedSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobAttemptFailedSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobAttemptStartedSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobCanceledSlot)) } just Runs
    every { dispatcher.toJobEngine(capture(jobCompletedSlot)) } just Runs
    every { dispatcher.toMonitoringPerName(capture(jobStatusUpdatedSlot)) } just Runs
    // given
    val engine = JobEngine()
    engine.logger = logger
    engine.storage = storage
    engine.dispatcher = dispatcher
    // when
    engine.handle(msgIn)
    // then
    val o = EngineResults()
    o.dispatcher = dispatcher
    o.storage = storage
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

    return o
}

internal class AvroJobEngineTests : StringSpec({
    "JobAttemptDispatched" {
        val stateIn = state()
        val msgIn = jobAttemptDispatched()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "JobAttemptStarted" {
        val stateIn = state()
        val msgIn = jobAttemptStarted()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "JobCompleted" {
        val stateIn = state()
        val msgIn = jobCompleted()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "JobCanceled" {
        val stateIn = state()
        val msgIn = jobCanceled()
        val o = engineHandle(stateIn, msgIn)
        checkShouldDoNothing(o)
    }

    "CancelJob" {
        val stateIn = state(mapOf("jobStatus" to JobStatus.RUNNING_OK))
        val msgIn = cancelJob(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.dispatcher.toJobEngine(o.jobCanceled!!)
            o.storage.deleteState(msgIn.jobId)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.jobCanceled!!.jobId shouldBe msgIn.jobId
        o.jobCanceled!!.jobMeta shouldBe stateIn.jobMeta
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_CANCELED
    }

    "DispatchJob" {
        val msgIn = dispatchJob()
        val o = engineHandle(null, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.dispatcher.toWorkers(o.workerMessage!!)
            o.dispatcher.toJobEngine(o.jobAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, null)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunJob) shouldBe true
        val run = o.workerMessage as RunJob
        run.jobId shouldBe msgIn.jobId
        run.jobName shouldBe msgIn.jobName
        run.jobInput shouldBe msgIn.jobInput
        run.jobAttemptRetry.int shouldBe 0
        run.jobAttemptId shouldBe o.jobAttemptDispatched!!.jobAttemptId
        run.jobAttemptRetry shouldBe o.jobAttemptDispatched!!.jobAttemptRetry
        o.state!!.jobId shouldBe msgIn.jobId
        o.state!!.jobName shouldBe msgIn.jobName
        o.state!!.jobInput shouldBe msgIn.jobInput
        o.state!!.jobAttemptId shouldBe run.jobAttemptId
        o.state!!.jobAttemptRetry.int shouldBe 0
        o.state!!.jobMeta shouldBe msgIn.jobMeta
        o.state!!.jobStatus shouldBe JobStatus.RUNNING_OK
        o.jobStatusUpdated!!.oldStatus shouldBe null
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_OK
    }

    "RetryJob" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_ERROR
            )
        )
        val msgIn = retryJob(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyAll {
            o.storage.getState(msgIn.jobId)
            o.dispatcher.toWorkers(o.workerMessage!!)
            o.dispatcher.toJobEngine(o.jobAttemptDispatched!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        (o.workerMessage is RunJob) shouldBe true
        val run = o.workerMessage as RunJob
        run.jobId shouldBe stateIn.jobId
        run.jobAttemptId shouldNotBe stateIn.jobAttemptId
        run.jobAttemptRetry.int shouldBe 0
        run.jobName shouldBe stateIn.jobName
        run.jobInput shouldBe stateIn.jobInput
        o.jobAttemptDispatched!!.jobId shouldBe stateIn.jobId
        o.jobAttemptDispatched!!.jobAttemptId shouldBe run.jobAttemptId
        o.jobAttemptDispatched!!.jobAttemptRetry.int shouldBe 0
        o.state!!.jobId shouldBe stateIn.jobId
        o.state!!.jobName shouldBe stateIn.jobName
        o.state!!.jobInput shouldBe stateIn.jobInput
        o.state!!.jobAttemptId shouldBe run.jobAttemptId
        o.state!!.jobAttemptRetry shouldBe run.jobAttemptRetry
        o.state!!.jobStatus shouldBe JobStatus.RUNNING_WARNING
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
    }

    "RetryJobAttempt" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_ERROR
            )
        )
        val msgIn = retryJobAttempt(
            mapOf(
                "jobId" to stateIn.jobId,
                "jobAttemptId" to stateIn.jobAttemptId,
                "jobAttemptRetry" to stateIn.jobAttemptRetry
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "JobAttemptCompleted" {
        val stateIn = state(mapOf("jobStatus" to JobStatus.RUNNING_OK))
        val msgIn = jobAttemptCompleted(mapOf("jobId" to stateIn.jobId))
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.dispatcher.toJobEngine(o.jobCompleted!!)
            o.storage.deleteState(msgIn.jobId)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
        o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
        o.jobStatusUpdated!!.newStatus shouldBe JobStatus.TERMINATED_COMPLETED
        o.jobCompleted!!.jobOutput shouldBe msgIn.jobOutput
        o.jobCompleted!!.jobMeta shouldBe stateIn.jobMeta
    }

    "JobAttemptFailed without retry" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_OK
            )
        )
        val msgIn = jobAttemptFailed(
            mapOf(
                "jobId" to stateIn.jobId,
                "jobAttemptId" to stateIn.jobAttemptId,
                "jobAttemptRetry" to stateIn.jobAttemptRetry,
                "jobAttemptDelayBeforeRetry" to null
            )
        )
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
        }
        checkConfirmVerified(o)
    }

    "JobAttemptFailed with future retry" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_OK
            )
        )
        val msgIn = jobAttemptFailed(
            mapOf(
                "jobId" to stateIn.jobId,
                "jobAttemptId" to stateIn.jobAttemptId,
                "jobAttemptRetry" to stateIn.jobAttemptRetry,
                "jobAttemptDelayBeforeRetry" to 42F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        verifyOrder {
            o.storage.getState(msgIn.jobId)
            o.dispatcher.toJobEngine(o.retryJobAttempt!!, o.retryJobAttemptDelay!!)
            o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
            o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
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

    "JobAttemptFailed with immediate retry" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_OK
            )
        )
        val msgIn = jobAttemptFailed(
            mapOf(
                "jobId" to stateIn.jobId,
                "jobAttemptId" to stateIn.jobAttemptId,
                "jobAttemptRetry" to stateIn.jobAttemptRetry,
                "jobAttemptDelayBeforeRetry" to 0F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }

    "JobAttemptFailed with immediate retry (negative delay)" {
        val stateIn = state(
            mapOf(
                "jobStatus" to JobStatus.RUNNING_OK
            )
        )
        val msgIn = jobAttemptFailed(
            mapOf(
                "jobId" to stateIn.jobId,
                "jobAttemptId" to stateIn.jobAttemptId,
                "jobAttemptRetry" to stateIn.jobAttemptRetry,
                "jobAttemptDelayBeforeRetry" to -42F
            )
        )
        val o = engineHandle(stateIn, msgIn)
        checkShouldRetryJobAttempt(msgIn, stateIn, o)
    }
})

private fun checkShouldDoNothing(o: EngineResults) {
    checkConfirmVerified(o)
}

private fun checkShouldRetryJobAttempt(msgIn: ForJobEngineMessage, stateIn: JobEngineState, o: EngineResults) {
    verifyOrder {
        o.storage.getState(msgIn.jobId)
        o.dispatcher.toWorkers(o.workerMessage!!)
        o.dispatcher.toJobEngine(o.jobAttemptDispatched!!)
        o.storage.updateState(msgIn.jobId, o.state!!, stateIn)
        o.dispatcher.toMonitoringPerName(o.jobStatusUpdated!!)
    }
    checkConfirmVerified(o)
    (o.workerMessage is RunJob) shouldBe true
    val run = o.workerMessage as RunJob
    run.jobId shouldBe stateIn.jobId
    run.jobAttemptId shouldBe stateIn.jobAttemptId
    run.jobAttemptRetry shouldBe stateIn.jobAttemptRetry + 1
    run.jobName shouldBe stateIn.jobName
    run.jobInput shouldBe stateIn.jobInput
    o.jobAttemptDispatched!!.jobId shouldBe stateIn.jobId
    o.jobAttemptDispatched!!.jobAttemptId shouldBe run.jobAttemptId
    o.jobAttemptDispatched!!.jobAttemptRetry shouldBe run.jobAttemptRetry
    o.state!!.jobId shouldBe stateIn.jobId
    o.state!!.jobName shouldBe stateIn.jobName
    o.state!!.jobInput shouldBe stateIn.jobInput
    o.state!!.jobAttemptId shouldBe run.jobAttemptId
    o.state!!.jobAttemptRetry shouldBe run.jobAttemptRetry
    o.state!!.jobStatus shouldBe JobStatus.RUNNING_WARNING
    o.jobStatusUpdated!!.oldStatus shouldBe stateIn.jobStatus
    o.jobStatusUpdated!!.newStatus shouldBe JobStatus.RUNNING_WARNING
}

private fun checkConfirmVerified(o: EngineResults) {
    confirmVerified(o.dispatcher)
    confirmVerified(o.storage)
    confirmVerified(o.logger)
}

private fun state(values: Map<String, Any?>? = null) = TestFactory.random(JobEngineState::class, values)

private fun cancelJob(values: Map<String, Any?>? = null) = TestFactory.random(CancelJob::class, values)
private fun dispatchJob(values: Map<String, Any?>? = null) = TestFactory.random(DispatchJob::class, values)
private fun retryJob(values: Map<String, Any?>? = null) = TestFactory.random(RetryJob::class, values)
private fun retryJobAttempt(values: Map<String, Any?>? = null) = TestFactory.random(RetryJobAttempt::class, values)

private fun jobCompleted(values: Map<String, Any?>? = null) = TestFactory.random(JobCompleted::class, values)
private fun jobCanceled(values: Map<String, Any?>? = null) = TestFactory.random(JobCanceled::class, values)
private fun jobAttemptDispatched(values: Map<String, Any?>? = null) = TestFactory.random(JobAttemptDispatched::class, values)
private fun jobAttemptCompleted(values: Map<String, Any?>? = null) = TestFactory.random(JobAttemptCompleted::class, values)
private fun jobAttemptFailed(values: Map<String, Any?>? = null) = TestFactory.random(JobAttemptFailed::class, values)
private fun jobAttemptStarted(values: Map<String, Any?>? = null) = TestFactory.random(JobAttemptStarted::class, values)
