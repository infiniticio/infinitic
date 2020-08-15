package com.zenaton.jobManager.tests

import com.zenaton.jobManager.client.Client
import com.zenaton.jobManager.client.Dispatcher
import com.zenaton.jobManager.engine.avroClasses.AvroJobEngine
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringGlobal
import com.zenaton.jobManager.engine.avroClasses.AvroMonitoringPerName
import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.data.Job
import com.zenaton.jobManager.data.AvroJobStatus
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
    val job = JobTestImpl()
    Worker.register(JobTest::class.java.name, job)

    beforeTest {
        storage.reset()
        JobTestImpl.log = ""
    }

    "Job succeeds at first try" {
        // job will succeed
        job.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            client.dispatch<JobTest> { log(1) }
        }
        // checks number of job processing
        JobTestImpl.log shouldBe "1END"
    }

    "Job succeeds at 4th try" {
        // job will succeed only at the 4th try
        job.behavior = { _ , retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS}
        // run system
        coroutineScope {
            dispatcher.scope = this
            client.dispatch<JobTest> { log(1) }
        }
        // checks number of job processing
        JobTestImpl.log shouldBe "1111END"
    }

    "Job fails" {
        val j: Job
        // job will succeed only at the 4th try
        job.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log(1) }
        }
        // checks number of job processing
        JobTestImpl.log shouldBe "1"
        // check that job is completed
        storage.jobEngineStore[j.jobId.id]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
    }

    "Job fails after 4 tries " {
        val j: Job
        // job will succeed only at the 4th try
        job.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            j = client.dispatch<JobTest> { log(1) }
        }
        // checks number of job processing
        JobTestImpl.log shouldBe "1111"
        // check that job is completed
        storage.jobEngineStore[j.jobId.id]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
    }

//    "Job succeeds after manual retry" {
//        // job will succeed only at the 4th try
//        job.behavior = { index, retry ->
//            if (index == 0)
//                if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
//            else
//                Status.SUCCESS
//        }
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            j = client.dispatch<JobTest> { log(1) }
//        }
//        // checks number of job processing
//        JobTestImpl.log shouldBe "1111"
//        // run system
//        val dispatch = getAvroDispatchJob()
//        val retry = getAvroRetryJob(dispatch.jobId)
//        coroutineScope {
//            dispatcher.scope = this
//            dispatcher.toJobEngine(dispatch)
//            delay(100)
//            dispatcher.toJobEngine(retry)
//        }
//        // check that job is completed
//        storage.jobEngineStore[dispatch.jobId] shouldBe null
//    }
//
//    "Job canceled during automatic retry" {
//        // job will succeed only at the 4th try
//        worker.behavior = { Status.FAILED_WITH_RETRY }
//        // run system
//        val dispatch = getAvroDispatchJob()
//        val retry = getAvroCancelJob(dispatch.jobId)
//        coroutineScope {
//            dispatcher.scope = this
//            dispatcher.toJobEngine(dispatch)
//            delay(100)
//            dispatcher.toJobEngine(retry)
//        }
//        // check that job is canceled
//        storage.jobEngineStore[dispatch.jobId] shouldBe null
//    }
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
