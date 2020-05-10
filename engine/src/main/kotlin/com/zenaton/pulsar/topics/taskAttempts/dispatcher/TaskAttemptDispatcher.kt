package com.zenaton.pulsar.topics.taskAttempts.dispatcher

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.messages.topics.taskAttempts.AvroTaskAttemptDispatched
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.taskAttempts.converter.TaskAttemptConverter
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context

object TaskAttemptDispatcher {
    fun dispatch(context: Context, msg: TaskAttemptDispatched) {
        context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(msg.getName()), JSONSchema.of(AvroTaskAttemptDispatched::class.java))
            .value(TaskAttemptConverter.toAvro(msg))
            .send()
    }
}
