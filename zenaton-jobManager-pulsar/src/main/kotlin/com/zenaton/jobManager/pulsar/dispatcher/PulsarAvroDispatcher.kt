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
 * This object provides methods to send messages to different topics
 */
class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {

    private var prefix = "jobs"

    // With topics prefix, it is possible to have different instances of jobManager in same tenant/namespace
    // UserConfigValue are set when functions are deployed on topics
    init {
        val prefix = context.getUserConfigValue("topicPrefix")
        if (prefix.isPresent) this.prefix = prefix.get().toString()
    }

    /**
     *  Dispatch messages to workers
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
     *  Dispatch messages to Global Monitoring
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
     *  Dispatch messages to Per Name Monitoring
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
     *  Dispatch messages to JobManager Engine
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
