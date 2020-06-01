package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.taskmanager.admin.messages.AvroTaskAdminMessage
import com.zenaton.taskmanager.admin.messages.TaskAdminMessage
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.engine.messages.AvroTaskEngineMessage
import com.zenaton.taskmanager.engine.messages.TaskEngineMessage
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.workers.AvroTaskWorkerMessage
import com.zenaton.taskmanager.workers.messages.TaskWorkerMessage
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class PulsarTaskDispatcher(val context: Context) : TaskDispatcher {

    /**
     *  Workers message
     */
    override fun dispatch(msg: TaskWorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.taskName.name), AvroSchema.of(AvroTaskWorkerMessage::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Admin message
     */
    override fun dispatch(msg: TaskAdminMessage) {
        context
            .newOutputMessage(Topic.ADMIN.get(), AvroSchema.of(AvroTaskAdminMessage::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun dispatch(msg: TaskMetricMessage) {
        context
            .newOutputMessage(Topic.METRICS.get(), AvroSchema.of(AvroTaskMetricMessage::class.java))
            .key(msg.taskName.name)
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun dispatch(msg: TaskEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroTaskEngineMessage::class.java))
            .key(msg.taskId.id)
            .value(TaskAvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
