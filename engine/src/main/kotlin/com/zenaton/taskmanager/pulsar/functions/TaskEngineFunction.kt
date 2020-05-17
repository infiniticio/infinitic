package com.zenaton.taskmanager.pulsar.functions

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.stater.TaskStater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class TaskEngineFunction : Function<AvroTaskMessage, Void> {

    // task engine injection
    var taskEngine = TaskEngine()
    // avro converter injection
    var avroConverter = TaskAvroConverter

    override fun process(input: AvroTaskMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            taskEngine.stater = TaskStater(ctx)
            taskEngine.dispatcher = TaskEngineDispatcher(ctx)
            taskEngine.logger = Logger(ctx)

            taskEngine.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
