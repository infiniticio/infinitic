package com.zenaton.jobManager.tests.inMemory

import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher as AvroEngineDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.worker.avroInterfaces.AvroDispatcher as AvroWorkerDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class InMemoryDispatcher : AvroWorkerDispatcher, AvroEngineDispatcher {
    // Here we favor lambda to avoid a direct dependency with engines instances
    lateinit var jobEngineHandle: (msg: AvroEnvelopeForJobEngine) -> Unit
    lateinit var monitoringPerNameHandle: (msg: AvroEnvelopeForMonitoringPerName) -> Unit
    lateinit var monitoringGlobalHandle: (msg: AvroEnvelopeForMonitoringGlobal) -> Unit
    lateinit var workerHandle: (msg: AvroEnvelopeForWorker) -> Unit
    lateinit var scope: CoroutineScope

    override fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            jobEngineHandle(msg)
        }
    }

    override fun toJobEngine(msg: AvroEnvelopeForJobEngine) = toJobEngine(msg, 0F)

    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        scope.launch {
            monitoringPerNameHandle(msg)
        }
    }

    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        scope.launch {
            monitoringGlobalHandle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        scope.launch {
            workerHandle(msg)
        }
    }
}
