package com.zenaton.jobManager.engines

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.avroEngines.AvroMonitoringGlobal
import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessageType
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import io.kotest.core.spec.style.ShouldSpec
import org.apache.avro.specific.SpecificRecordBase
import java.lang.Exception

private val engine = AvroJobEngine()
private val perName = AvroMonitoringPerName()
private val global = AvroMonitoringGlobal()
private val worker = SyncWorker()
private val dispatcher = SyncAvroDispatcher(engine, perName, global, worker)

class EngineScenariiTests : ShouldSpec({

    context("TaskMetrics.handle") {
//        val avroStorage = SyncAvroStorage()
//        val avroDispatcher = SyncAvroDispatcher()
//        engine.avroStorage = avroStorage
//        engine.avroDispatcher = avroDispatcher
//        perName.avroStorage = avroStorage
//        perName.avroDispatcher = avroDispatcher
//        global.avroStorage = avroStorage
//        worker.avroDispatcher = avroDispatcher

        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
        }
    }
})

internal class SyncAvroDispatcher(
    private val avroJobEngine: AvroJobEngine,
    private val avroMonitoringPerName: AvroMonitoringPerName,
    private val avroMonitoringGlobal: AvroMonitoringGlobal,
    private val worker: SyncWorker
) : AvroDispatcher {
    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        TODO("Not yet implemented")
    }

    override fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float) {
        avroJobEngine.handle(msg)
    }

    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        avroMonitoringPerName.handle(msg)
    }

    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        avroMonitoringGlobal.handle(msg)
    }
}

class SyncWorker() {
    lateinit var avroDispatcher: AvroDispatcher

    fun handle(msg: AvroEnvelopeForWorker) {
        val avro = AvroConverter.fromWorkers(msg)
        when (avro) {
            is RunJob -> {
                val avroJobAttemptStarted = AvroJobAttemptStarted.newBuilder()
                    .setJobAttemptId(avro.jobAttemptId.id)
                    .build()
            }
        }
    }
}

private fun removeEnvelopeForWorker(msg: AvroEnvelopeForWorker) = when (msg.type!!) {
    AvroForWorkerMessageType.RunJob -> msg.runJob
}

private fun addEnvelopeForWorker(msg: SpecificRecordBase): AvroEnvelopeForWorker = when (msg) {
    is AvroRunJob ->
        AvroEnvelopeForWorker.newBuilder()
            .setJobName(msg.jobName)
            .setType(AvroForWorkerMessageType.RunJob)
            .setRunJob(msg)
            .build()
    else -> throw Exception("Unknown msg $msg")
}

private class SyncAvroStorage : AvroStorage {
    var engineStore: Map<String, AvroJobEngineState> = mapOf()
    var perNameStore: Map<String, AvroMonitoringPerNameState> = mapOf()
    var globalStore: AvroMonitoringGlobalState? = null

    override fun getJobEngineState(jobId: String): AvroJobEngineState? = engineStore[jobId]

    override fun updateJobEngineState(jobId: String, newState: AvroJobEngineState, oldState: AvroJobEngineState?) {
        engineStore = engineStore.plus(jobId to newState)
    }

    override fun deleteJobEngineState(jobId: String) {
        engineStore.minus(jobId)
    }

    override fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState? = perNameStore[jobName]

    override fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        perNameStore = perNameStore.plus(jobName to newState)
    }

    override fun deleteMonitoringPerNameState(jobName: String) {
        perNameStore.minus(jobName)
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? = globalStore

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        globalStore = newState
    }

    override fun deleteMonitoringGlobalState() {
        globalStore = null
    }
}
