package com.zenaton.jobManager.avroEngines.inMemory

import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class InMemoryDispatcher : AvroDispatcher {
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
