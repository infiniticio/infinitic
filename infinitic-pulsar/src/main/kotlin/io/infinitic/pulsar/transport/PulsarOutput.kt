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
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.messageBuilders.sendPulsarMessageAsync
import io.infinitic.pulsar.topics.GlobalTopic
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarOutput(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    pulsarTenant: String,
    pulsarNamespace: String
) {
    private val logger = KotlinLogging.logger {}

    private val topicNamer = TopicName(pulsarTenant, pulsarNamespace)
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
        logger.debug { "topic: $topic, sendToClient: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskTagEngine(topicType: TopicType, async: Boolean = false): SendToTaskTagEngine = { message ->
        val taskTopic = when (topicType) {
            TopicType.NEW -> TaskTopic.TAG_NEW
            TopicType.EXISTING -> TaskTopic.TAG_EXISTING
        }
        val topic = topicNamer.of(taskTopic, "$message.taskName")
        val key = "${message.taskTag}"
        logger.debug { "topic: $topic, sendToTaskTagEngine: $message" }
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToTaskEngine(topicType: TopicType, name: Name? = null, async: Boolean = false): SendToTaskEngine = { message ->
        val topic = when (name) {
            is WorkflowName -> {
                val workflowTaskTopic = when (topicType) {
                    TopicType.NEW -> WorkflowTaskTopic.ENGINE_NEW
                    TopicType.EXISTING -> WorkflowTaskTopic.ENGINE_EXISTING
                }
                topicNamer.of(workflowTaskTopic, "$name")
            }
            is TaskName -> {
                val taskTopic = when (topicType) {
                    TopicType.NEW -> TaskTopic.ENGINE_NEW
                    TopicType.EXISTING -> TaskTopic.ENGINE_EXISTING
                }
                topicNamer.of(taskTopic, "$name")
            }
            null -> {
                val taskTopic = when (topicType) {
                    TopicType.NEW -> TaskTopic.ENGINE_NEW
                    TopicType.EXISTING -> TaskTopic.ENGINE_EXISTING
                }
                topicNamer.of(taskTopic, "${message.taskName}")
            }
            else -> throw thisShouldNotHappen()
        }
        val key = "${message.taskId}"
        logger.debug { "topic: $topic, sendToTaskEngine: $message" }
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToTaskEngineAfter(name: Name? = null): SendToTaskEngineAfter = { message, after ->
        val topic = when (name) {
            is WorkflowName -> topicNamer.of(WorkflowTaskTopic.DELAYS, "$name")
            is TaskName -> topicNamer.of(TaskTopic.DELAYS, "$name")
            null -> topicNamer.of(TaskTopic.DELAYS, "${message.taskName}")
            else -> throw thisShouldNotHappen()
        }
        val key = null
        logger.debug { "topic: $topic, sendToTaskEngineAfter: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToWorkflowTagEngine(topicType: TopicType, async: Boolean = false): SendToWorkflowTagEngine = { message ->
        val workflowTopic = when (topicType) {
            TopicType.NEW -> WorkflowTopic.TAG_NEW
            TopicType.EXISTING -> WorkflowTopic.TAG_EXISTING
        }
        val topic = topicNamer.of(workflowTopic, "${message.workflowName}")
        val key = "${message.workflowTag}"
        logger.debug { "topic: $topic, sendToWorkflowTagEngine: $message" }
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToWorkflowEngine(topicType: TopicType, async: Boolean = false): SendToWorkflowEngine = { message ->
        val workflowTopic = when (topicType) {
            TopicType.NEW -> WorkflowTopic.ENGINE_NEW
            TopicType.EXISTING -> WorkflowTopic.ENGINE_EXISTING
        }
        val topic = topicNamer.of(workflowTopic, "${message.workflowName}")
        val key = "${message.workflowId}"
        logger.debug { "topic: $topic, sendToWorkflowEngine: $message" }
        when (async) {
            true -> pulsarMessageBuilder.sendPulsarMessageAsync(topic, message.envelope(), key, zero)
            false -> pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
        }
    }

    fun sendToWorkflowEngineAfter(): SendToWorkflowEngineAfter = { message, after ->
        val topic = topicNamer.of(WorkflowTopic.DELAYS, "${message.workflowName}")
        val key = null
        logger.debug { "topic: $topic, sendToWorkflowEngineAfter: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToTaskExecutors(name: Name? = null): SendToTaskExecutors = { message ->
        val topic = when (name) {
            is WorkflowName -> topicNamer.of(WorkflowTaskTopic.EXECUTORS, "$name")
            is TaskName -> topicNamer.of(TaskTopic.EXECUTORS, "$name")
            null -> topicNamer.of(TaskTopic.EXECUTORS, "${message.taskName}")
            else -> throw thisShouldNotHappen()
        }
        val key = null
        logger.debug { "topic: $topic, sendToTaskExecutors: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToMetricsPerName(name: Name? = null): SendToMetricsPerName = { message: MetricsPerNameMessage ->
        val topic = when (name) {
            is WorkflowName -> topicNamer.of(WorkflowTaskTopic.METRICS, "$name")
            is TaskName -> topicNamer.of(TaskTopic.METRICS, "$name")
            null -> topicNamer.of(TaskTopic.METRICS, "${message.taskName}")
            else -> throw thisShouldNotHappen()
        }
        val key = null
        logger.debug { "topic: $topic, sendToMetricsPerName: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToMetricsGlobal(): SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        val topic = topicNamer.of(GlobalTopic.METRICS)
        val key = null
        logger.debug { "topic: $topic, sendToMetricsGlobal: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }
}
