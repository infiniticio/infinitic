package com.zenaton.pulsar.topics.taskAttempts.dispatcher

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.messages.taskAttempts.AvroTaskAttemptDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.utils.AvroConverter
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

object TaskAttemptDispatcher {
    fun dispatch(context: Context, msg: TaskAttemptDispatched): MessageId {
        return context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(msg.taskName.name), AvroSchema.of(AvroTaskAttemptDispatched::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }
}
