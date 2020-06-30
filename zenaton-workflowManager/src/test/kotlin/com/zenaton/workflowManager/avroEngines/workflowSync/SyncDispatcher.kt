package com.zenaton.workflowManager.avroEngines.workflowSync

import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.workflowManager.avroEngines.AvroWorkflowEngine
import com.zenaton.workflowManager.avroEngines.jobSync.SyncWorkerDecision
import com.zenaton.workflowManager.avroEngines.jobSync.SyncWorker
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SyncDispatcher(
    private val workflowEngine: AvroWorkflowEngine,
    private val jobEngine: AvroJobEngine,
    private val decider: SyncWorkerDecision,
    private val worker: SyncWorker
) : AvroDispatcher {
    lateinit var scope: CoroutineScope

    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            workflowEngine.handle(msg)
        }
    }

    override fun toDeciders(msg: AvroEnvelopeForJobEngine) {
        scope.launch {
            jobEngine.handle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForJobEngine) {
        scope.launch {
            jobEngine.handle(msg)
        }
    }
}
