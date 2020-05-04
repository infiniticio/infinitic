package com.zenaton.pulsar.topics.tasks.functions

import com.zenaton.engine.tasks.data.TaskState
import com.zenaton.engine.tasks.functions.TaskEngine
import com.zenaton.pulsar.topics.tasks.dispatcher.TaskDispatcher
import com.zenaton.pulsar.topics.tasks.messages.TaskMessageContainer
import com.zenaton.pulsar.utils.Logger
import com.zenaton.pulsar.utils.Stater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEngineFunction : Function<TaskMessageContainer, Void> {

    override fun process(input: TaskMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            val msg = input.msg()

            TaskEngine(
                stater = Stater<TaskState>(ctx),
                dispatcher = TaskEngineDispatcher(ctx),
                logger = Logger(ctx)
            ).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
