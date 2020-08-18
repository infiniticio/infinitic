package com.zenaton.jobManager.tests

import com.zenaton.jobManager.client.Client
import com.zenaton.jobManager.engine.avroClasses.AvroJobEngine
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringGlobal
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringPerName
import com.zenaton.jobManager.common.data.Job
import com.zenaton.jobManager.data.AvroJobStatus
import com.zenaton.jobManager.tests.inMemory.InMemoryDispatcher
import com.zenaton.jobManager.tests.inMemory.InMemoryStorage
import com.zenaton.jobManager.worker.Worker
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
private val worker = Worker()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

private lateinit var status: AvroJobStatus

class AvroEngineTests : StringSpec({
    val jobTest = JobTestImpl()
    Worker.register<JobTest>(jobTest)
    var job: Job

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
            job = client.dispatch<JobTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(job) shouldBe true
        // check that job is completed
        status shouldBe AvroJobStatus.TERMINATED_COMPLETED
        // checks number of job processing
        JobTestImpl.log shouldBe "1"
    }

    "Job succeeds at 4th try" {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            job = client.dispatch<JobTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(job) shouldBe true
        // check that job is completed
        status shouldBe AvroJobStatus.TERMINATED_COMPLETED
        // checks number of job processing
        JobTestImpl.log shouldBe "0001"
    }

    "Job fails at first try" {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            job = client.dispatch<JobTest> { log() }
        }
        // check that job is not terminated
        storage.isTerminated(job) shouldBe false
        // check that job is failed
        status shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processing
        JobTestImpl.log shouldBe "0"
    }

    "Job fails after 4 tries " {
        // job will succeed only at the 4th try
        jobTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            job = client.dispatch<JobTest> { log() }
        }
        // check that job is not terminated
        storage.isTerminated(job) shouldBe false
        // check that job is failed
        status shouldBe AvroJobStatus.RUNNING_ERROR
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
            job = client.dispatch<JobTest> { log() }
            delay(100)
            client.retry(id = job.jobId.id)
        }
        // check that job is terminated
        storage.isTerminated(job)
        // check that job is completed
        status shouldBe AvroJobStatus.TERMINATED_COMPLETED
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
            job = client.dispatch<JobTest> { log() }
            delay(100)
            client.cancel(id = job.jobId.id)
        }
        // check that job is terminated
        storage.isTerminated(job)
        // check that job is completed
        status shouldBe AvroJobStatus.TERMINATED_CANCELED
    }
}) {
    init {
        client.setAvroDispatcher(dispatcher)

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

        worker.setAvroDispatcher(dispatcher)

        dispatcher.apply {
            jobEngineHandle = { jobEngine.handle(it) }
            monitoringPerNameHandle = { avro ->
                monitoringPerName.handle(avro)
                avro.jobStatusUpdated?.let { status = it.newStatus }
            }
            monitoringGlobalHandle = { monitoringGlobal.handle(it) }
            workerHandle = { worker.handle(it) }
        }
    }
}
