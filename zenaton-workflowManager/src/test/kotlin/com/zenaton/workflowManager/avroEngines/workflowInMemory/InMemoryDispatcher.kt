package com.zenaton.workflowManager.avroEngines.workflowInMemory

import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.workflowManager.avroEngines.AvroWorkflowEngine
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class InMemoryDispatcher(
    private val workflowEngine: AvroWorkflowEngine,
    private val decisionEngine: AvroJobEngine,
    private val taskEngine: AvroJobEngine
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
            decisionEngine.handle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForJobEngine) {
        scope.launch {
            taskEngine.handle(msg)
        }
    }
}
