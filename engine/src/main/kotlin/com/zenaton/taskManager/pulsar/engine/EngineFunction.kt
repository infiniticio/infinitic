package com.zenaton.taskManager.pulsar.engine

import com.zenaton.jobManager.engine.messages.AvroEngineMessage
import com.zenaton.taskManager.engine.Engine
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.taskManager.pulsar.logger.PulsarLogger
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class EngineFunction : Function<AvroEngineMessage, Void> {

    // task engine injection
    var taskEngine = Engine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroEngineMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskEngine.logger = PulsarLogger(ctx)
        taskEngine.taskDispatcher = PulsarDispatcher(ctx)
        taskEngine.workflowDispatcher = WorkflowDispatcher(ctx)
        taskEngine.storage = EnginePulsarStorage(ctx)

        try {
            taskEngine.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            taskEngine.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
