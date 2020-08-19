package com.zenaton.taskManager.pulsar.dispatcher

import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForWorker
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides methods to send messages to different topics
 */
class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {

    private var prefix = "tasks"

    // With topics prefix, it is possible to have different instances of taskManager in same tenant/namespace
    // UserConfigValue are set when functions are deployed on topics
    init {
        val prefix = context.getUserConfigValue("topicPrefix")
        if (prefix.isPresent) this.prefix = prefix.get().toString()
    }

    /**
     *  Dispatch messages to workers
     */
    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        context
            .newOutputMessage(Topic.WORKERS.get(prefix, msg.taskName), AvroSchema.of(AvroEnvelopeForWorker::class.java))
            .value(msg)
            .send()
        context.logger.debug("===============TaskManager====================")
        context.logger.debug("Topic: ${Topic.WORKERS.get(prefix, msg.taskName)}")
        context.logger.debug("Msg: $msg")
    }

    /**
     *  Dispatch messages to Global Monitoring
     */
    override fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(prefix), AvroSchema.of(AvroEnvelopeForMonitoringGlobal::class.java))
            .value(msg)
            .send()
        context.logger.debug("===============TaskManager====================")
        context.logger.debug("Topic: ${Topic.MONITORING_GLOBAL.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }

    /**
     *  Dispatch messages to Per Name Monitoring
     */
    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(prefix), AvroSchema.of(AvroEnvelopeForMonitoringPerName::class.java))
            .key(msg.taskName)
            .value(msg)
            .send()
        context.logger.debug("===============TaskManager====================")
        context.logger.debug("Topic: ${Topic.MONITORING_PER_NAME.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }

    /**
     *  Dispatch messages to TaskManager Engine
     */
    override fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.TASK_ENGINE.get(prefix), AvroSchema.of(AvroEnvelopeForTaskEngine::class.java))
            .key(msg.taskId)
            .value(msg)

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()

        context.logger.debug("===============TaskManager====================")
        context.logger.debug("Topic: ${Topic.TASK_ENGINE.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }
}
