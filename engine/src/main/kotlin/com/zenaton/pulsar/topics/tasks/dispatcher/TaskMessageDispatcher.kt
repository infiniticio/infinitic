package com.zenaton.pulsar.topics.tasks.dispatcher

import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface
import com.zenaton.messages.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.tasks.converter.TaskMessageConverter
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
object TaskMessageDispatcher {

    fun dispatch(context: Context, msg: TaskMessageInterface, after: Float = 0f): MessageId {

        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), AvroSchema.of(AvroTaskMessage::class.java))
            .key(msg.getKey())
            .value(TaskMessageConverter.toAvro(msg))

//        if (after > 0) {
//            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
//        }

        return msgBuilder.send()
    }
}
