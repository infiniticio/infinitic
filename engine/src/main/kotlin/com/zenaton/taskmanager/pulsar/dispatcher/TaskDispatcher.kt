package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.taskmanager.dispatcher.TaskDispatcherInterface
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.AvroTaskStatusUpdated
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.AvroRunTask
import com.zenaton.taskmanager.messages.workers.RunTask
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class TaskDispatcher(val context: Context) : TaskDispatcherInterface {

    /**
     *  Workers message
     */
    override fun dispatch(msg: RunTask) {
        context
            .newOutputMessage(Topic.TASK_ATTEMPTS.get(msg.taskName.name), AvroSchema.of(AvroRunTask::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun dispatch(msg: TaskStatusUpdated) {
        context
            .newOutputMessage(Topic.TASK_STATUS_UPDATES.get(), AvroSchema.of(AvroTaskStatusUpdated::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun dispatch(msg: TaskEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.TASKS.get(), AvroSchema.of(AvroTaskEngineMessage::class.java))
            .key(msg.getStateId())
            .value(TaskAvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
