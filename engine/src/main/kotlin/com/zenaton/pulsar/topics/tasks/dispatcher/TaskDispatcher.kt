package com.zenaton.pulsar.topics.tasks.dispatcher

import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface
import com.zenaton.messages.topics.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.tasks.converter.TaskConverter
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

object TaskDispatcher {

    fun dispatch(context: Context, msg: TaskMessageInterface, after: Float = 0f) {

        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), AvroSchema.of(AvroTaskMessage::class.java))
            .key(msg.getKey())
            .value(TaskConverter.toAvro(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()
    }
}
