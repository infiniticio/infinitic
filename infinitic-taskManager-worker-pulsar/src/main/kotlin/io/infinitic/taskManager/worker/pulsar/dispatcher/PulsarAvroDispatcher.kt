package io.infinitic.taskManager.worker.pulsar.dispatcher

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.worker.AvroDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {
    override suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine) {
        withContext(Dispatchers.IO) {
            context.newOutputMessage(Topic.TASK_ENGINE.get("tasks"), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
                .key(msg.taskId)
                .value(msg)
                .send()
        }
    }
}
