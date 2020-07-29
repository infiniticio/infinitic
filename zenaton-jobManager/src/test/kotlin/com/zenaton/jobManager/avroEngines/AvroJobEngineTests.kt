package com.zenaton.jobManager.avroEngines

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryDispatcher
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryStorage
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryWorker.Status
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryWorkerJob
import com.zenaton.jobManager.data.AvroJobStatus
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)
private val jobEngine = AvroJobEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = InMemoryWorkerJob()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

class AvroEngineTests : StringSpec({
    beforeTest {
        worker.jobA = mockk()
        every { worker.jobA.handle() } just Runs
        storage.reset()
    }

    "Job succeeds at first try" {
        // job will succeed
        worker.behavior = { Status.COMPLETED }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
        // checks number of job processing
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job succeeds at 4th try" {
        // job will succeed only at the 4th try
        worker.behavior = { job -> if (job.jobAttemptRetry < 3) Status.FAILED_WITH_RETRY else Status.COMPLETED }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
        // checks number of job processing
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job fails" {
        // job will succeed only at the 4th try
        worker.behavior = { Status.FAILED_WITHOUT_RETRY }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processing
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job fails after 4 trys " {
        // job will succeed only at the 4th try
        worker.behavior = { job ->
            if (job.jobAttemptIndex == 0)
                if (job.jobAttemptRetry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            else
                Status.COMPLETED
        } // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processing
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job succeeds after manual retry" {
        // job will succeed only at the 4th try
        worker.behavior = { job ->
            if (job.jobAttemptIndex == 0)
                if (job.jobAttemptRetry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            else
                Status.COMPLETED
        }
        // run system
        val dispatch = getAvroDispatchJob()
        val retry = getAvroRetryJob(dispatch.jobId)
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
            delay(100)
            dispatcher.toJobEngine(retry)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
    }

    "Job canceled during automatic retry" {
        // job will succeed only at the 4th try
        worker.behavior = { job -> Status.FAILED_WITH_RETRY }
        // run system
        val dispatch = getAvroDispatchJob()
        val retry = getAvroCancelJob(dispatch.jobId)
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
            delay(100)
            dispatcher.toJobEngine(retry)
        }
        // check that job is canceled
        storage.jobEngineStore[dispatch.jobId] shouldBe null
    }
}) {
    init {
        jobEngine.apply {
            avroStorage = storage
            avroDispatcher = dispatcher
            logger = mockLogger
        }

        monitoringPerName.apply {
            avroStorage = storage
            avroDispatcher = dispatcher
            logger = mockLogger
        }

        monitoringGlobal.apply {
            avroStorage = storage
            logger = mockLogger
        }

        worker.avroDispatcher = dispatcher

        dispatcher.apply {
            jobEngineHandle = { jobEngine.handle(it) }
            monitoringPerNameHandle = { monitoringPerName.handle(it) }
            monitoringGlobalHandle = { monitoringGlobal.handle(it) }
            workerHandle = { worker.handle(it) }
        }
    }
}

private fun getAvroDispatchJob() = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroDispatchJob::class, mapOf("jobName" to "JobA"))
)

private fun getAvroRetryJob(id: String) = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroRetryJob::class, mapOf("jobId" to id))
)

private fun getAvroCancelJob(id: String) = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroCancelJob::class, mapOf("jobId" to id))
)
