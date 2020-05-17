package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
object TaskDispatcher {

    fun dispatch(context: Context, msg: RunTask) {
        context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(msg.taskName.name), AvroSchema.of(AvroRunTask::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    fun dispatch(context: Context, msg: TaskMessageInterface, after: Float = 0f) {

        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), AvroSchema.of(AvroTaskMessage::class.java))
            .key(msg.getStateId())
            .value(TaskAvroConverter.toAvro(msg))

        if (after > 0f) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
