package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.topics.tasks.engine.TaskEngine
import com.zenaton.engine.topics.tasks.state.TaskState
import com.zenaton.messages.topics.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.tasks.converter.TaskConverter
import com.zenaton.pulsar.utils.Logger
import com.zenaton.pulsar.utils.Stater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEngineFunction : Function<AvroTaskMessage, Void> {

    override fun process(input: AvroTaskMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            val msg = TaskConverter.fromAvro(input)

            TaskEngine(
                stater = Stater<TaskState>(ctx),
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
