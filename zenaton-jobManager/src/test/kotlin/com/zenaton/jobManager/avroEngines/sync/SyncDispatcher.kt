package com.zenaton.jobManager.avroEngines.sync

import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.avroEngines.AvroMonitoringGlobal
import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SyncDispatcher(
    private val jobEngine: AvroJobEngine,
    private val monitoringPerName: AvroMonitoringPerName,
    private val monitoringGlobal: AvroMonitoringGlobal,
    private val worker: SyncWorker
) : AvroDispatcher {
    lateinit var scope: CoroutineScope

    override fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            jobEngine.handle(msg)
        }
    }

    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        scope.launch {
            monitoringPerName.handle(msg)
        }
    }

    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        scope.launch {
            monitoringGlobal.handle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        scope.launch {
            worker.handle(msg)
        }
    }
}
