package com.zenaton.jobManager.tests

import com.zenaton.jobManager.client.Client
import com.zenaton.jobManager.client.Dispatcher
import com.zenaton.jobManager.engine.avroClasses.AvroJobEngine
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringGlobal
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringPerName
import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.Job
import com.zenaton.jobManager.tests.inMemory.InMemoryDispatcher
import com.zenaton.jobManager.tests.inMemory.InMemoryStorage
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.tests.utils.TestFactory
import com.zenaton.jobManager.worker.Worker
import com.zenaton.jobManager.worker.avroClasses.AvroWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)
private val client = Client()
private val jobEngine = AvroJobEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = AvroWorker()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

class AvroEngineTests : StringSpec({
    val jobTest = JobTestImpl()
    Worker.register<JobTest>(jobTest)
    var j: Job

    beforeTest {
        storage.reset()
        JobTestImpl.log = ""
    }

    "Job succeeds at first try" {
        // job will succeed
        jobTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(j)
        // checks number of job processing
        JobTestImpl.log shouldBe "1"
    }

    "Job succeeds at 4th try" {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(j)
        // checks number of job processing
        JobTestImpl.log shouldBe "0001"
    }

    "Job fails at first try" {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
        }
        // check that job is failed
        storage.isFailed(j)
        // checks number of job processing
        JobTestImpl.log shouldBe "0"
    }

    "Job fails after 4 tries " {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
        }
        // check that job is failed
        storage.isFailed(j)
        // checks number of job processing
        JobTestImpl.log shouldBe "0000"
    }

    "Job succeeds after manual retry" {
        // job will succeed only at the 4th try
        jobTest.behavior = { index, retry ->
            if (index == 0)
                if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            else if (retry < 2) Status.FAILED_WITH_RETRY else Status.SUCCESS
        }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
            delay(100)
            client.retry(id = j.jobId.id)
        }
        // check that job is terminated
        storage.isTerminated(j)
        // checks number of job processing
        JobTestImpl.log shouldBe "0000001"
    }

    "Job canceled during automatic retry" {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log() }
            delay(100)
            client.cancel(id = j.jobId.id)
        }
        // check that job is completed
        storage.isTerminated(j)
    }
}) {
    init {
        client.dispatcher = Dispatcher(dispatcher)

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
