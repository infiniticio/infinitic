package io.infinitic.workflowManager.dispatcher.pulsar

import io.infinitic.taskManager.dispatcher.pulsar.Wrapper
import io.infinitic.taskManager.dispatcher.pulsar.wrapper.PulsarClientWrapper
import io.infinitic.taskManager.dispatcher.pulsar.wrapper.PulsarFunctionContextWrapper
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.workflowManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit
import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher as BaseDispatcher

class PulsarDispatcher(wrapper: Wrapper) : BaseDispatcher(wrapper), AvroDispatcher {

    override suspend fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float) {
        withContext(Dispatchers.IO) {
            val messageBuilder = wrapper
                .newMessage(Topic.WORKFLOW_ENGINE.get("workflows"), AvroSchema.of(AvroEnvelopeForWorkflowEngine::class.java))
                .key(msg.workflowId)
                .value(msg)

            if (after > 0) {
                messageBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
            }

            messageBuilder.send()
        }
    }

    override suspend fun toDeciders(msg: AvroEnvelopeForTaskEngine) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.TASK_ENGINE.get("decisions"), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
                .key(msg.taskId)
                .value(msg)
                .send()
        }
    }

    override suspend fun toWorkers(msg: AvroEnvelopeForTaskEngine) {
        withContext(Dispatchers.IO) {
            wrapper
                .newMessage(Topic.TASK_ENGINE.get("tasks"), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
                .key(msg.taskId)
                .value(msg)
                .send()
        }
    }

    companion object {
        fun forPulsarClient(pulsarClient: PulsarClient): PulsarDispatcher = PulsarDispatcher(PulsarClientWrapper(pulsarClient))
        fun forPulsarFunctionContext(pulsarFunctionContext: Context): PulsarDispatcher = PulsarDispatcher(PulsarFunctionContextWrapper(pulsarFunctionContext))
    }
}
