package com.zenaton.jobManager.pulsar.engine

import com.zenaton.jobManager.engine.Engine
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.jobManager.pulsar.logger.PulsarLogger
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class EngineFunction : Function<AvroForEngineMessage, Void> {

    // task engine injection
    var taskEngine = Engine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroForEngineMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskEngine.logger = PulsarLogger(ctx)
        taskEngine.dispatch = PulsarDispatcher(ctx)
        taskEngine.workflowDispatcher = WorkflowDispatcher(ctx)
        taskEngine.storage = EnginePulsarStorage(ctx)

        try {
            taskEngine.handle(avroConverter.fromAvroForEngineMessage(input))
        } catch (e: Exception) {
            taskEngine.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
