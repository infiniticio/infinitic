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

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.Name
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.clientTopic
import io.infinitic.pulsar.topics.getPersistentTopicFullName
import io.infinitic.pulsar.topics.globalMetricsTopic
import io.infinitic.pulsar.topics.taskEngineTopic
import io.infinitic.pulsar.topics.taskExecutorTopic
import io.infinitic.pulsar.topics.taskMetricsTopic
import io.infinitic.pulsar.topics.taskTagEngineTopic
import io.infinitic.pulsar.topics.workflowEngineTopic
import io.infinitic.pulsar.topics.tagEngineTopic
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarOutput(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(
            pulsarClient: PulsarClient,
            pulsarTenant: String,
            pulsarNamespace: String,
            name: String
        ) = PulsarOutput(
            PulsarMessageBuilderFromClient(pulsarClient, name),
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

    fun sendToClient(): SendToClient =
        { message: ClientMessage ->
            val topic = getPersistentTopicFullName(
                pulsarTenant,
                pulsarNamespace,
                clientTopic(message.clientName)
            )
            logger.debug("topic: {}, sendToClient: {}", topic, message)
            pulsarMessageBuilder.sendPulsarMessage(
                topic,
                message.envelope(),
                null,
                MillisDuration(0)
            )
        }

    fun sendToTaskTagEngine(topicType: TopicType): SendToTaskTagEngine =
        { message: TaskTagEngineMessage ->
            val topic = getPersistentTopicFullName(
                pulsarTenant,
                pulsarNamespace,
                taskTagEngineTopic(topicType, message.taskName)
            )
            logger.debug("topic: {}, sendToTaskTagEngine: {}", topic, message)
            pulsarMessageBuilder.sendPulsarMessage(
                topic,
                message.envelope(),
                "${message.taskTag}",
                MillisDuration(0)
            )
        }

    fun sendToTaskEngine(topicType: TopicType, name: Name? = null): SendToTaskEngine =
        { message: TaskEngineMessage, after: MillisDuration ->
            val topic = getPersistentTopicFullName(
                pulsarTenant,
                pulsarNamespace,
                taskEngineTopic(topicType, name ?: message.taskName)
            )
            logger.debug("topic: {}, sendToTaskEngine: {}", topic, message)
            pulsarMessageBuilder.sendPulsarMessage(
                topic,
                message.envelope(),
                "${message.taskId}",
                after
            )
        }

    fun sendToWorkflowTagEngine(topicType: TopicType): SendToWorkflowTagEngine =
        { message: WorkflowTagEngineMessage ->
            val topic = getPersistentTopicFullName(
                pulsarTenant,
                pulsarNamespace,
                tagEngineTopic(topicType, message.workflowName)
            )
            logger.debug("topic: {}, sendToWorkflowTagEngine: {}", topic, message)
            pulsarMessageBuilder.sendPulsarMessage(
                topic,
                message.envelope(),
                "${message.workflowTag}",
                MillisDuration(0)
            )
        }

    fun sendToWorkflowEngine(topicType: TopicType): SendToWorkflowEngine =
        { message: WorkflowEngineMessage, after: MillisDuration ->
            val topic = getPersistentTopicFullName(
                pulsarTenant,
                pulsarNamespace,
                workflowEngineTopic(topicType, message.workflowName)
            )
            logger.debug("topic: {}, sendToWorkflowEngine: {}", topic, message)
            pulsarMessageBuilder.sendPulsarMessage(
                topic,
                message.envelope(),
                "${message.workflowId}",
                after
            )
        }

    fun sendToTaskExecutors(name: Name? = null): SendToTaskExecutors = { message: TaskExecutorMessage ->
        val topic = getPersistentTopicFullName(
            pulsarTenant,
            pulsarNamespace,
            taskExecutorTopic(name ?: message.taskName)
        )
        logger.debug("topic: {}, sendToTaskExecutors: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(
            topic,
            message.envelope(),
            "${message.taskId}",
            MillisDuration(0)
        )
    }

    fun sendToMetricsPerName(): SendToMetricsPerName = { message: MetricsPerNameMessage ->
        val topic = getPersistentTopicFullName(
            pulsarTenant,
            pulsarNamespace,
            taskMetricsTopic(message.taskName)
        )
        logger.debug("topic: {}, sendToMetricsPerName: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(
            topic,
            message.envelope(),
            "${message.taskName}",
            MillisDuration(0)
        )
    }

    fun sendToMetricsGlobal(): SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        val topic = getPersistentTopicFullName(
            pulsarTenant,
            pulsarNamespace,
            globalMetricsTopic()
        )
        logger.debug("topic: {}, sendToMetricsGlobal: {}", topic, message)
        pulsarMessageBuilder.sendPulsarMessage(
            topic,
            message.envelope(),
            null,
            MillisDuration(0)
        )
    }
}
