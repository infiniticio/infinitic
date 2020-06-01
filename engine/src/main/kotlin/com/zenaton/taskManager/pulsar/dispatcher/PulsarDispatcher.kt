package com.zenaton.taskManager.pulsar.dispatcher

import com.zenaton.taskManager.admin.messages.AvroMonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.engine.messages.AvroTaskEngineMessage
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.pulsar.Topic
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.workers.AvroTaskWorkerMessage
import com.zenaton.taskManager.workers.WorkerMessage
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class PulsarDispatcher(val context: Context) : Dispatcher {

    /**
     *  Workers message
     */
    override fun dispatch(msg: WorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.taskName.name), AvroSchema.of(AvroTaskWorkerMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Admin message
     */
    override fun dispatch(msg: MonitoringGlobalMessage) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(), AvroSchema.of(AvroMonitoringGlobalMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun dispatch(msg: MonitoringPerNameMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(), AvroSchema.of(AvroMonitoringPerNameMessage::class.java))
            .key(msg.taskName.name)
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun dispatch(msg: EngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroTaskEngineMessage::class.java))
            .key(msg.taskId.id)
            .value(AvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
