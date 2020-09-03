package io.infinitic.taskManager.tests.inMemory

import io.infinitic.taskManager.client.AvroTaskDispatcher as AvroTaskClientDispatcher
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher as AvroTaskEngineDispatcher
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.workflowManager.engine.avroInterfaces.AvroDispatcher as AvroWorkflowEngineDispatcher
import io.infinitic.taskManager.worker.AvroDispatcher as AvroWorkerDispatcher
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.client.AvroWorkflowDispatcher as AvroWorkflowClientDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class InMemoryDispatcher :
    AvroWorkerDispatcher,
    AvroTaskEngineDispatcher,
    AvroTaskClientDispatcher,
    AvroWorkflowEngineDispatcher,
    AvroWorkflowClientDispatcher {
    // Here we favor lambda to avoid a direct dependency with engines instances
    lateinit var workflowEngineHandle: (msg: AvroEnvelopeForWorkflowEngine) -> Unit
    lateinit var taskEngineHandle: (msg: AvroEnvelopeForTaskEngine) -> Unit
    lateinit var monitoringPerNameHandle: (msg: AvroEnvelopeForMonitoringPerName) -> Unit
    lateinit var monitoringGlobalHandle: (msg: AvroEnvelopeForMonitoringGlobal) -> Unit
    lateinit var workerHandle: suspend (msg: AvroEnvelopeForWorker) -> Unit

    lateinit var scope: CoroutineScope

    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            workflowEngineHandle(msg)
        }
    }

    override fun toDeciders(msg: AvroEnvelopeForTaskEngine) {
        scope.launch {
            taskEngineHandle(msg)
        }
    }

    override fun toWorkers(msg: AvroEnvelopeForTaskEngine) {
        scope.launch {
            taskEngineHandle(msg)
        }
    }

    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine) {
        scope.launch {
            workflowEngineHandle(msg)
        }
    }

    override fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            taskEngineHandle(msg)
        }
    }

    override suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine) = toTaskEngine(msg, 0F)

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
