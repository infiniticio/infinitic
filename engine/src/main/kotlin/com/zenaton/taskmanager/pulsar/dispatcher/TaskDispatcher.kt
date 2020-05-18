package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.taskmanager.dispatcher.TaskDispatcherInterface
import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.RunTask
import com.zenaton.taskmanager.pulsar.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.Topic
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class TaskDispatcher(val context: Context) : TaskDispatcherInterface {

    fun dispatch(msg: RunTask) {
        context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(msg.taskName.name), AvroSchema.of(AvroRunTask::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    override fun dispatch(msg: TaskMessageInterface, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), AvroSchema.of(AvroTaskMessage::class.java))
            .key(msg.getStateId())
            .value(TaskAvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
