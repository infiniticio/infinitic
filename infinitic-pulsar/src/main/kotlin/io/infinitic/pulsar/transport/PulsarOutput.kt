/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.transport

import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.Name
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.messageBuilders.sendPulsarMessageAsync
import io.infinitic.pulsar.topics.TopicNamer
import io.infinitic.pulsar.topics.TopicType
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarOutput(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    pulsarTenant: String,
    pulsarNamespace: String
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    private val topicNamer = TopicNamer(pulsarTenant, pulsarNamespace)
    private val zero = MillisDuration(0)

    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(
            pulsarClient: PulsarClient,
            pulsarTenant: String,
            pulsarNamespace: String,
            producerName: String
        ) = PulsarOutput(
            PulsarMessageBuilderFromClient(pulsarClient, producerName),
            pulsarTenant,
            pulsarNamespace
        )

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarOutput(
            PulsarMessageBuilderFromFunction(context),
            context.tenant,
            context.namespace
        )
    }

    fun sendToClient(): SendToClient = { message ->
        val topic = topicNamer.clientTopic(message.clientName)
        val key = null
        logger.debug("topic: {}, sendToClient: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskTagEngine(topicType: TopicType, async: Boolean = false): SendToTaskTagEngine = { message ->
        val topic = topicNamer.tagEngineTopic(topicType, message.taskName)
        val key = "${message.taskTag}"
        logger.debug("topic: {}, sendToTaskTagEngine: {}", topic, message)
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToTaskEngine(topicType: TopicType, name: Name? = null, async: Boolean = false): SendToTaskEngine = { message ->
        val topic = topicNamer.taskEngineTopic(topicType, name ?: message.taskName)
        val key = "${message.taskId}"
        logger.debug("topic: {}, sendToTaskEngine: {}", topic, message)
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToTaskEngineAfter(name: Name? = null): SendToTaskEngineAfter = { message, after ->
        val topic = topicNamer.delayEngineTopic(name ?: message.taskName)
        val key = null
        logger.debug("topic: {}, sendToTaskEngineAfter: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToWorkflowTagEngine(topicType: TopicType, async: Boolean = false): SendToWorkflowTagEngine = { message ->
        val topic = topicNamer.tagEngineTopic(topicType, message.workflowName)
        val key = "${message.workflowTag}"
        logger.debug("topic: {}, sendToWorkflowTagEngine: {}", topic, message)
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToWorkflowEngine(topicType: TopicType, async: Boolean = false): SendToWorkflowEngine = { message ->
        val topic = topicNamer.workflowEngineTopic(topicType, message.workflowName)
        val key = "${message.workflowId}"
        logger.debug("topic: {}, sendToWorkflowEngine: {}", topic, message)
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToWorkflowEngineAfter(): SendToWorkflowEngineAfter = { message, after ->
        val topic = topicNamer.delayEngineTopic(message.workflowName)
        val key = null
        logger.debug("topic: {}, sendToWorkflowEngineAfter: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToTaskExecutors(name: Name? = null): SendToTaskExecutors = { message ->
        val topic = topicNamer.executorTopic(name ?: message.taskName)
        val key = null
        logger.debug("topic: {}, sendToTaskExecutors: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToMetricsPerName(): SendToMetricsPerName = { message: MetricsPerNameMessage ->
        val topic = topicNamer.metricsTopic(message.taskName)
        val key = null
        logger.debug("topic: {}, sendToMetricsPerName: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToMetricsGlobal(): SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        val topic = topicNamer.globalMetricsTopic()
        val key = null
        logger.debug("topic: {}, sendToMetricsGlobal: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }
}
