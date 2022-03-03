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
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.metrics.global.SendToGlobalMetrics
import io.infinitic.common.metrics.global.messages.GlobalMetricsMessage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.SendToTaskEngineAfter
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.metrics.SendToTaskMetrics
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.transport.pulsar.topics.GlobalTopic
import io.infinitic.transport.pulsar.topics.TaskTopic
import io.infinitic.transport.pulsar.topics.TopicName
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopic
import io.infinitic.transport.pulsar.topics.WorkflowTopic
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarOutput(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    pulsarTenant: String,
    pulsarNamespace: String
) {
    private val logger = KotlinLogging.logger {}

    private val topicName = TopicName(pulsarTenant, pulsarNamespace)
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
        val topic = topicName.of(message.recipientName)
        val key = null
        logger.debug { "topic: $topic, sendToClient: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskTagEngine(): SendToTaskTag = { message ->
        val topic = topicName.of(TaskTopic.TAG, "${message.taskName}")
        val key = "${message.taskTag}"
        logger.debug { "topic: $topic, sendToTaskTagEngine: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskEngine(): SendToTaskEngine = { message ->
        val topic = topicName.of(TaskTopic.ENGINE, "${message.taskName}")
        val key = "${message.taskId}"
        logger.debug { "topic: $topic, sendToTaskEngine: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskEngineAfter(name: Name? = null): SendToTaskEngineAfter = { message, after ->
        val topic = when (name) {
            is WorkflowName -> topicName.of(WorkflowTaskTopic.DELAYS, "$name")
            is TaskName -> topicName.of(TaskTopic.DELAYS, "$name")
            null -> topicName.of(TaskTopic.DELAYS, "${message.taskName}")
            else -> thisShouldNotHappen()
        }
        val key = null
        logger.debug { "topic: $topic, sendToTaskEngineAfter: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToWorkflowTaskEngine(workflowName: WorkflowName): SendToTaskEngine = { message ->
        val topic = topicName.of(WorkflowTaskTopic.ENGINE, "$workflowName")
        val key = "${message.taskId}"
        logger.debug { "topic: $topic, sendToTaskEngine: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToWorkflowTagEngine(): SendToWorkflowTag = { message ->
        val topic = topicName.of(WorkflowTopic.TAG, "${message.workflowName}")
        val key = "${message.workflowTag}"
        logger.debug { "topic: $topic, sendToWorkflowTagEngine: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToWorkflowEngine(): SendToWorkflowEngine = { message ->
        val topic = topicName.of(WorkflowTopic.ENGINE, "${message.workflowName}")
        val key = "${message.workflowId}"
        logger.debug { "topic: $topic, sendToWorkflowEngine: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToWorkflowEngineAfter(): SendToWorkflowEngineAfter = { message, after ->
        val topic = topicName.of(WorkflowTopic.DELAYS, "${message.workflowName}")
        val key = null
        logger.debug { "topic: $topic, sendToWorkflowEngineAfter: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, after)
    }

    fun sendToTaskExecutors(name: Name? = null): SendToTaskExecutor = { message ->
        val topic = when (name) {
            is WorkflowName -> topicName.of(WorkflowTaskTopic.EXECUTORS, "$name")
            is TaskName -> topicName.of(TaskTopic.EXECUTORS, "$name")
            null -> topicName.of(TaskTopic.EXECUTORS, "${message.taskName}")
            else -> thisShouldNotHappen()
        }
        val key = null
        logger.debug { "topic: $topic, sendToTaskExecutors: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToTaskMetrics(name: Name? = null): SendToTaskMetrics = { message: TaskMetricsMessage ->
        val topic = when (name) {
            is WorkflowName -> topicName.of(WorkflowTaskTopic.METRICS, "$name")
            is TaskName -> topicName.of(TaskTopic.METRICS, "$name")
            null -> topicName.of(TaskTopic.METRICS, "${message.taskName}")
            else -> thisShouldNotHappen()
        }
//        val key = null
        logger.debug { "topic: $topic, sendToTaskMetrics: $message" }
//        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }

    fun sendToGlobalMetrics(): SendToGlobalMetrics = { message: GlobalMetricsMessage ->
        val topic = topicName.of(GlobalTopic.METRICS)
        val key = null
        logger.debug { "topic: $topic, sendToGlobalMetrics: $message" }
        pulsarMessageBuilder.sendPulsarMessage(topic, message.envelope(), key, zero)
    }
}
