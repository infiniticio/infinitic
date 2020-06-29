package com.zenaton.jobManager.avroEngines

import com.zenaton.common.data.AvroSerializedData
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.lang.StringBuilder

private val logger = mockk<Logger>(relaxed = true)
private val engine = AvroJobEngine()
private val perName = AvroMonitoringPerName()
private val global = AvroMonitoringGlobal()
private val worker = SyncWorker()
private val avroDispatcher = SyncAvroDispatcher(engine, perName, global, worker)
private val avroStorage = SyncAvroStorage()

class AvroEngineTests : ShouldSpec({

    context("q") {


        should("q") {
            val avro = TestFactory.random(
                AvroDispatchJob::class,
                mapOf("jobName" to "JobA", "jobMeta" to mapOf<String, String>())
            )

            runBlocking {
                avroDispatcher.scope = this
                avroDispatcher.toJobEngine(AvroConverter.addEnvelopeToJobEngineMessage(avro))
            }

            avroStorage.engineStore shouldBe mapOf()
            worker.log shouldBe """
JobA failed
JobA failed
JobA failed
JobA failed
JobA failed
JobA failed
JobA completed
"""

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
        worker.avroDispatcher = avroDispatcher
    }
}

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
        scope.launch() {
            avroMonitoringPerName.handle(msg)
        }
    }

    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        scope.launch() {
            avroMonitoringGlobal.handle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        scope.launch() {
            worker.handle(msg)
        }
    }
}

class SyncWorker() {
    lateinit var avroDispatcher: AvroDispatcher
    var log = "\n"

    fun logJobStarted(name: String, input: List<AvroSerializedData>) {
        log += "$name"
    }
    fun logJobCompleted(name: String) {
        log += " completed\n"
    }
    fun logJobFailed(name: String) {
        log += " failed\n"
    }

    suspend fun handle(msg: AvroEnvelopeForWorker) {
        val avro = AvroConverter.removeEnvelopeFromWorkerMessage(msg)
        when (avro) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                logJobStarted(avro.jobName, avro.jobInput)
                if (avro.jobAttemptRetry < 6) {
                    sendJobFailed(avro, Exception("Try Again!"), 1F)
                    logJobFailed(avro.jobName)
                } else {
                    sendJobCompleted(avro, "${avro.jobName} output")
                    logJobCompleted(avro.jobName)
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
