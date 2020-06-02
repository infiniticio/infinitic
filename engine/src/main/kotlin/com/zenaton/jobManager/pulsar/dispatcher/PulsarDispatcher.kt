package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.pulsar.Topic
import com.zenaton.jobManager.pulsar.avro.AvroConverter
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
    override fun toWorkers(msg: ForWorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.jobName.name), AvroSchema.of(AvroForWorkerMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Admin message
     */
    override fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(), AvroSchema.of(AvroForMonitoringGlobalMessage::class.java))
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(), AvroSchema.of(AvroForMonitoringPerNameMessage::class.java))
            .key(msg.jobName.name)
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerInstance(msg: ForMonitoringPerInstanceMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_INSTANCE.get(), AvroSchema.of(AvroForMonitoringPerInstanceMessage::class.java))
            .key(msg.jobId.id)
            .value(AvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun toEngine(msg: ForEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroForEngineMessage::class.java))
            .key(msg.jobId.id)
            .value(AvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}
