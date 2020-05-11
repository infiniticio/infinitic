package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.topics.tasks.engine.TaskEngine
import com.zenaton.messages.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.tasks.TaskStater
import com.zenaton.pulsar.topics.tasks.converter.TaskMessageConverter
import com.zenaton.pulsar.utils.Logger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class TaskEngineFunction : Function<AvroTaskMessage, Void> {

    override fun process(input: AvroTaskMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            val msg = TaskMessageConverter.fromAvro(input)

            TaskEngine(
                stater = TaskStater(ctx),
                dispatcher = TaskEngineDispatcher(ctx),
                logger = Logger(ctx)
            ).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
