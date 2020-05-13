package com.zenaton.taskmanager.pulsar.functions

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.pulsar.avro.AvroConverter
import com.zenaton.taskmanager.pulsar.stater.TaskStater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class TaskEngineFunction : Function<AvroTaskMessage, Void> {

    override fun process(input: AvroTaskMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            val msg = AvroConverter.fromAvro(input)

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
