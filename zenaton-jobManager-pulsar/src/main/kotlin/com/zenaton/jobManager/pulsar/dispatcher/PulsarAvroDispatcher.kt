package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroForEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage
import com.zenaton.workflowManager.messages.envelopes.AvroForEngineMessage as AvroForWorkflowsMessage
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {

    private var prefix = "jobs"

    // Set topics prefix to handle multiple installation of jobManager
    init {
        val prefix = context.getUserConfigValue("topicPrefix")
        if (prefix.isPresent) this.prefix = prefix.get().toString()
    }

    /**
     *  Workers message
     */
    override fun toWorkers(msg: AvroForWorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(prefix, msg.jobName), AvroSchema.of(AvroForWorkerMessage::class.java))
            .value(msg)
            .send()
        context.logger.info("===============JobManager====================")
        context.logger.info("Topic: ${Topic.WORKERS.get(prefix, msg.jobName)}")
        context.logger.info("Msg: $msg")
    }

    /**
     *  Admin message
     */
    override fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(prefix), AvroSchema.of(AvroForMonitoringGlobalMessage::class.java))
            .value(msg)
            .send()
        context.logger.info("===============JobManager====================")
        context.logger.info("Topic: ${Topic.MONITORING_GLOBAL.get(prefix)}")
        context.logger.info("Msg: $msg")
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(prefix), AvroSchema.of(AvroForMonitoringPerNameMessage::class.java))
            .key(msg.jobName)
            .value(msg)
            .send()
        context.logger.info("===============JobManager====================")
        context.logger.info("Topic: ${Topic.MONITORING_PER_NAME.get(prefix)}")
        context.logger.info("Msg: $msg")
    }

    /**
     *  Engine messages
     */
    override fun toEngine(msg: AvroForEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(prefix), AvroSchema.of(AvroForEngineMessage::class.java))
            .key(msg.jobId)
            .value(msg)

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()

        context.logger.info("===============JobManager====================")
        context.logger.info("Topic: ${Topic.ENGINE.get(prefix)}")
        context.logger.info("Msg: $msg")
    }

    override fun toWorkflows(msg: AvroForWorkflowsMessage) {
        TODO("Not yet implemented")
    }
}
