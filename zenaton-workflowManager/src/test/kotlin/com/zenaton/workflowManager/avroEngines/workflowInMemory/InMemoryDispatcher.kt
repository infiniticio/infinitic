package com.zenaton.workflowManager.avroEngines.workflowInMemory

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class InMemoryDispatcher : AvroDispatcher {
    lateinit var workflowEngineHandle: (msg: AvroEnvelopeForWorkflowEngine) -> Unit
    lateinit var decisionEngineHandle: (msg: AvroEnvelopeForJobEngine) -> Unit
    lateinit var taskEngineHandle: (msg: AvroEnvelopeForJobEngine) -> Unit
    lateinit var scope: CoroutineScope

    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            workflowEngineHandle(msg)
        }
    }

    override fun toDeciders(msg: AvroEnvelopeForJobEngine) {
        scope.launch {
            decisionEngineHandle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForJobEngine) {
        scope.launch {
            taskEngineHandle(msg)
        }
    }
}
