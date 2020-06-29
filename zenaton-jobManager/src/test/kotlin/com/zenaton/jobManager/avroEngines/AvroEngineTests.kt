package com.zenaton.jobManager.avroEngines

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.data.AvroJobStatus
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger

private val logger = mockk<Logger>(relaxed = true)
private val engine = AvroJobEngine()
private val perName = AvroMonitoringPerName()
private val global = AvroMonitoringGlobal()
private val worker = SyncWorker()
private val avroDispatcher = SyncAvroDispatcher(engine, perName, global, worker)
private val avroStorage = SyncAvroStorage()

enum class Status {
    SUCCESS,
    FAIL_WITH_RETRY,
    FAIL_WITHOUT_RETRY
}

class AvroEngineTests : StringSpec({
    "Job succeeds at first try" {
        beforeTest()
        // job will succeed
        worker.behavior = { Status.SUCCESS }
        // run system
        val dispatch = getAvroDispatchJob()
        runBlocking {
            avroDispatcher.scope = this
            avroDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        avroStorage.engineStore.get(dispatch.jobId)?.jobStatus shouldBe null
        // checks number of job processings
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job succeeds at 5th try" {
        beforeTest()
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob -> if (job.jobAttemptRetry < 3) Status.FAIL_WITH_RETRY else Status.SUCCESS }
        // run system
        val dispatch = getAvroDispatchJob()
        runBlocking {
            avroDispatcher.scope = this
            avroDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        avroStorage.engineStore[dispatch.jobId]?.jobStatus shouldBe null
        // checks number of job processings
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job fails" {
        beforeTest()
        // job will succeed only at the 4th try
        worker.behavior = { Status.FAIL_WITHOUT_RETRY }
        // run system
        val dispatch = getAvroDispatchJob()
        runBlocking {
            avroDispatcher.scope = this
            avroDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        avroStorage.engineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processings
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job fails after 4 trys " {
        beforeTest()
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob -> if (job.jobAttemptRetry < 3) Status.FAIL_WITH_RETRY else Status.FAIL_WITHOUT_RETRY }
        // run system
        val dispatch = getAvroDispatchJob()
        runBlocking {
            avroDispatcher.scope = this
            avroDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        avroStorage.engineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processings
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job succeeds after manual retry" {
        beforeTest()
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob ->
            if (job.jobAttemptIndex == 0)
                if (job.jobAttemptRetry < 3)
                    Status.FAIL_WITH_RETRY
                else
                    Status.FAIL_WITHOUT_RETRY
            else
                Status.SUCCESS
        }
        // run system
        val dispatch = getAvroDispatchJob()
        val retry = getAvroRetryJob(dispatch.jobId)
        runBlocking {
            avroDispatcher.scope = this
            avroDispatcher.toJobEngine(dispatch)
            delay(100)
            avroDispatcher.toJobEngine(retry)
        }
        // check that job is completed
        avroStorage.engineStore[dispatch.jobId]?.jobStatus shouldBe null
        // checks number of job processings
        verify(exactly = 5) {
            worker.jobA.handle()
        }
    }
}) {
    init {
        engine.avroStorage = avroStorage
        engine.avroDispatcher = avroDispatcher
        engine.logger = logger
        perName.avroStorage = avroStorage
        perName.avroDispatcher = avroDispatcher
        perName.logger = logger
        global.avroStorage = avroStorage
        global.logger = logger
        worker.avroDispatcher = avroDispatcher
    }
}

private fun beforeTest() {
    worker.jobA = mockk()
    every { worker.jobA.handle() } just Runs
    avroStorage.init()
}

private fun getAvroDispatchJob() = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroDispatchJob::class, mapOf("jobName" to "JobA"))
)

private fun getAvroRetryJob(id: String) = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroRetryJob::class, mapOf("jobId" to id))
)

internal class SyncAvroDispatcher(
    private val avroJobEngine: AvroJobEngine,
    private val avroMonitoringPerName: AvroMonitoringPerName,
    private val avroMonitoringGlobal: AvroMonitoringGlobal,
    private val worker: SyncWorker
) : AvroDispatcher {
    lateinit var scope: CoroutineScope

    override fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float) {
        scope.launch {
            avroJobEngine.handle(msg)
        }
    }

    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        scope.launch {
            avroMonitoringPerName.handle(msg)
        }
    }

    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        scope.launch {
            avroMonitoringGlobal.handle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        scope.launch {
            worker.handle(msg)
        }
    }
}

class JobA { fun handle() {} }

class SyncWorker {
    lateinit var avroDispatcher: AvroDispatcher
    lateinit var behavior: (msg: AvroRunJob) -> Status?
    var jobA = JobA()

    fun handle(msg: AvroEnvelopeForWorker) {
        when (val avro = AvroConverter.removeEnvelopeFromWorkerMessage(msg)) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                val out = when (avro.jobName) {
                    "JobA" -> jobA.handle()
                    else -> throw Exception("Unknown job ${avro.jobName}")
                }
                when (behavior(avro)) {
                    Status.SUCCESS -> sendJobCompleted(avro, out)
                    Status.FAIL_WITH_RETRY -> sendJobFailed(avro, Exception("Will Try Again"), 1F)
                    Status.FAIL_WITHOUT_RETRY -> sendJobFailed(avro, Exception("Failed"))
                }
            }
        }
    }

    private fun sendJobStarted(avro: AvroRunJob) {
        // send start
        val avroJobAttemptStarted = AvroJobAttemptStarted.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptStarted))
    }

    private fun sendJobFailed(avro: AvroRunJob, error: Exception? = null, delay: Float? = null) {
        // send failure
        val avroJobAttemptFailed = AvroJobAttemptFailed.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .setJobAttemptError(AvroConverter.toAvroSerializedData(SerializedData.from(error)))
            .setJobAttemptDelayBeforeRetry(delay)
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptFailed))
    }

    private fun sendJobCompleted(avro: AvroRunJob, out: Any? = null) {
        // send completion
        val avroJobAttemptCompleted = AvroJobAttemptCompleted.newBuilder()
            .setJobId(avro.jobId)
            .setJobAttemptId(avro.jobAttemptId)
            .setJobAttemptRetry(avro.jobAttemptRetry)
            .setJobAttemptIndex(avro.jobAttemptIndex)
            .setJobOutput(AvroConverter.toAvroSerializedData(SerializedData.from(out)))
            .build()
        avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avroJobAttemptCompleted))
    }
}

private class SyncAvroStorage : AvroStorage {
    var engineStore: Map<String, AvroJobEngineState> = mapOf()
    var perNameStore: Map<String, AvroMonitoringPerNameState> = mapOf()
    var globalStore: AvroMonitoringGlobalState? = null

    fun init() {
        engineStore = mapOf()
        perNameStore = mapOf()
        globalStore = null
    }

    override fun getJobEngineState(jobId: String): AvroJobEngineState? {
        return engineStore[jobId]
    }

    override fun updateJobEngineState(jobId: String, newState: AvroJobEngineState, oldState: AvroJobEngineState?) {
        engineStore = engineStore.plus(jobId to newState)
    }

    override fun deleteJobEngineState(jobId: String) {
        engineStore = engineStore.minus(jobId)
    }

    override fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState? = perNameStore[jobName]

    override fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        perNameStore = perNameStore.plus(jobName to newState)
    }

    override fun deleteMonitoringPerNameState(jobName: String) {
        perNameStore = perNameStore.minus(jobName)
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? = globalStore

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        globalStore = newState
    }

    override fun deleteMonitoringGlobalState() {
        globalStore = null
    }
}
