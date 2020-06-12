package com.zenaton.jobManager.pulsar.dispatcher

import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage
import com.zenaton.jobManager.pulsar.Topic
import com.zenaton.workflowManager.messages.envelopes.AvroForWorkflowEngineMessage
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class PulsarAvroDispatcher(val context: Context) : AvroDispatcher {

    /**
     *  Workers message
     */
    override fun toWorkers(msg: AvroForWorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.jobName), AvroSchema.of(AvroForWorkerMessage::class.java))
            .value(msg)
            .send()
    }

    /**
     *  Admin message
     */
    override fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage) {
        context
            .newOutputMessage(Topic.MONITORING_GLOBAL.get(), AvroSchema.of(AvroForMonitoringGlobalMessage::class.java))
            .value(msg)
            .send()
    }

    /**
     *  Metrics message
     */
    override fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage) {
        context
            .newOutputMessage(Topic.MONITORING_PER_NAME.get(), AvroSchema.of(AvroForMonitoringPerNameMessage::class.java))
            .key(msg.jobName)
            .value(msg)
            .send()
    }

    /**
     *  Engine messages
     */
    override fun toJobEngine(msg: AvroForJobEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroForJobEngineMessage::class.java))
            .key(msg.jobId)
            .value(msg)

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }

    override fun toWorkflowEngine(msg: AvroForWorkflowEngineMessage) {
        TODO("Not yet implemented")
    }
}
