package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
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
    override fun toWorkers(msg: AvroEnvelopeForWorker) {
        context
            .newOutputMessage(Topic.WORKERS.get(prefix, msg.jobName), AvroSchema.of(AvroEnvelopeForWorker::class.java))
            .value(msg)
            .send()
        context.logger.debug("===============JobManager====================")
        context.logger.debug("Topic: ${Topic.WORKERS.get(prefix, msg.jobName)}")
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
        context.logger.debug("===============JobManager====================")
        context.logger.debug("Topic: ${Topic.MONITORING_GLOBAL.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }

    /**
     *  Dispatch messages to Per Name Monitoring
     */
    override fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(prefix), AvroSchema.of(AvroEnvelopeForMonitoringPerName::class.java))
            .key(msg.jobName)
            .value(msg)
            .send()
        context.logger.debug("===============JobManager====================")
        context.logger.debug("Topic: ${Topic.MONITORING_PER_NAME.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }

    /**
     *  Dispatch messages to JobManager Engine
     */
    override fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(prefix), AvroSchema.of(AvroEnvelopeForJobEngine::class.java))
            .key(msg.jobId)
            .value(msg)

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
        msgBuilder.send()

        context.logger.debug("===============JobManager====================")
        context.logger.debug("Topic: ${Topic.ENGINE.get(prefix)}")
        context.logger.debug("Msg: $msg")
    }

    override fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine) {
        TODO("Not yet implemented")
    }
}
