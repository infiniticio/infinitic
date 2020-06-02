package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.messages.monitoring.global.AvroMonitoringGlobalMessage
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.engine.EngineMessage
import com.zenaton.jobManager.messages.engine.AvroEngineMessage
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.monitoring.perName.AvroMonitoringPerNameMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.jobManager.pulsar.Topic
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.workers.AvroWorkerMessage
import com.zenaton.jobManager.workers.WorkerMessage
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
    override fun toWorkers(msg: WorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.jobName.name), AvroSchema.of(AvroWorkerMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Admin message
     */
    override fun toMonitoringGlobal(msg: MonitoringGlobalMessage) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(), AvroSchema.of(AvroMonitoringGlobalMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerName(msg: MonitoringPerNameMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(), AvroSchema.of(AvroMonitoringPerNameMessage::class.java))
            .key(msg.jobName.name)
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerInstance(msg: MonitoringPerInstanceMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_INSTANCE.get(), AvroSchema.of(AvroMonitoringPerInstanceMessage::class.java))
            .key(msg.jobId.id)
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    override fun toMonitoringPerInstance(msg: EngineMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_INSTANCE.get(), AvroSchema.of(AvroMonitoringPerInstanceMessage::class.java))
            .key(msg.jobId.id)
            .value(AvroConverter.toMonitoringPerInstanceAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun toEngine(msg: EngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroEngineMessage::class.java))
            .key(msg.jobId.id)
            .value(AvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
