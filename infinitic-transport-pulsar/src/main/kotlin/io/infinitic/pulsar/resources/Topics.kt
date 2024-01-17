/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.pulsar.resources

import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventEnvelope
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.topics.DelayedWorkflowServiceExecutorTopic
import io.infinitic.common.topics.NamingTopic
import io.infinitic.common.topics.ServiceEventsTopic
import io.infinitic.common.topics.ServiceExecutorTopic
import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.ServiceTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowCmdTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowEventsTopic
import io.infinitic.common.topics.WorkflowServiceEventsTopic
import io.infinitic.common.topics.WorkflowServiceExecutorTopic
import io.infinitic.common.topics.WorkflowTagTopic
import io.infinitic.common.topics.WorkflowTopic
import io.infinitic.common.workflows.engine.events.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.api.Schema
import kotlin.reflect.KClass

/**
 * Property indicating whether a topic type receives delayed message
 * This is used to set up TTLInSeconds which mush be much higher for delayed messages
 *
 * @return `true` if the topic is delayed, `false` otherwise.
 */
internal val Topic<*>.isDelayed
  get() = when (this) {
    DelayedWorkflowEngineTopic,
    DelayedWorkflowServiceExecutorTopic,
    DelayedServiceExecutorTopic
    -> true

    else -> false
  }

/**
 * Determines whether a topic type is partitioned.
 * This is used when creating a topic
 *
 * @return true if the topic is partitioned, false otherwise
 */
internal val Topic<*>.isPartitioned
  get() = when (this) {
    is NamingTopic, is ClientTopic -> false
    else -> true
  }

/**
 * Determines the prefix string per topic type
 *
 * @return The prefix string for the topic.
 */
fun Topic<*>.prefix() = when (this) {
  WorkflowTagTopic -> "workflow-tag"
  WorkflowCmdTopic -> "workflow-cmd"
  WorkflowEngineTopic -> "workflow-engine"
  DelayedWorkflowEngineTopic -> "workflow-delay"
  WorkflowEventsTopic -> "workflow-events"
  WorkflowServiceExecutorTopic, DelayedWorkflowServiceExecutorTopic -> "workflow-task-executor"
  WorkflowServiceEventsTopic -> "workflow-task-events"
  ServiceTagTopic -> "task-tag"
  ServiceExecutorTopic, DelayedServiceExecutorTopic -> "task-executor"
  ServiceEventsTopic -> "task-events"
  ClientTopic -> "response"
  NamingTopic -> "namer"
}

/**
 * Determines the prefix string per topic type for the dead letter queues
 *
 * @return The prefix string for the topic.
 */
internal fun Topic<*>.prefixDLQ() = "${prefix()}-dlq"


/**
 * Returns the name of the Topic based on topic type and the entity name
 *
 * @param entity The optional entity name (service name or workflow name).
 * @return The name of the Topic with optional entity name.
 */
internal fun Topic<*>.name(entity: String?) = prefix() + (entity?.let { ":$entity" } ?: "")

/**
 * Returns the name of the Dead Letter Queue Topic based on topic type and the entity name
 *
 * @param entity The optional entity name (service name or workflow name).
 * @return The name of the Topic with optional entity name.
 */
internal fun Topic<*>.nameDLQ(entity: String) = "${prefixDLQ()}:$entity"


/**
 * Returns the envelope class associated with a topic type.
 *
 * @return The envelope class that is associated with the topic.
 */
@Suppress("UNCHECKED_CAST")
internal val <S : Message> Topic<S>.envelopeClass: KClass<Envelope<out S>>
  get() = when (this) {
    WorkflowTagTopic -> WorkflowTagEnvelope::class
    WorkflowCmdTopic -> WorkflowEngineEnvelope::class
    WorkflowEngineTopic -> WorkflowEngineEnvelope::class
    DelayedWorkflowEngineTopic -> WorkflowEngineEnvelope::class
    WorkflowEventsTopic -> WorkflowEventEnvelope::class
    WorkflowServiceExecutorTopic, DelayedWorkflowServiceExecutorTopic -> ServiceExecutorEnvelope::class
    WorkflowServiceEventsTopic -> ServiceEventEnvelope::class
    ServiceTagTopic -> TaskTagEnvelope::class
    ServiceExecutorTopic, DelayedServiceExecutorTopic -> ServiceExecutorEnvelope::class
    ServiceEventsTopic -> ServiceEventEnvelope::class
    ClientTopic -> ClientEnvelope::class
    NamingTopic -> thisShouldNotHappen()
  } as KClass<Envelope<out S>>

/**
 * Returns the Avro schema associated with a topic type.
 *
 * @param S The type of the message contained in the topic.
 * @return The schema of the topic.
 */
internal val <S : Message> Topic<S>.schema: Schema<Envelope<out S>>
  get() = Schema.AVRO(schemaDefinition(envelopeClass))


/**
 * Retrieves the service name from a given topic name.
 *
 * @param topicName The name of the topic.
 * @return The service name or null if the topic name does not match any service.
 */
internal fun getServiceNameFromTopicName(topicName: String): String? {
  for (serviceTopic in ServiceTopic.entries) {
    val prefix = serviceTopic.prefix()
    if (topicName.startsWith(prefix)) return topicName.removePrefix(prefix)

    val prefixDLQ = serviceTopic.prefixDLQ()
    if (topicName.startsWith(prefixDLQ)) return topicName.removePrefix(prefixDLQ)
  }

  return null
}

/**
 * Retrieves the workflow name from the given topic name.
 *
 * @param topicName The topic name from which to extract the workflow name.
 * @return The extracted workflow name, or null if no matching workflow topic prefix is found.
 */
internal fun getWorkflowNameFromTopicName(topicName: String): String? {
  for (workflowTopic in WorkflowTopic.entries) {
    val prefix = workflowTopic.prefix()
    if (topicName.startsWith(prefix)) return topicName.removePrefix(prefix)

    val prefixDLQ = workflowTopic.prefixDLQ()
    if (topicName.startsWith(prefixDLQ)) return topicName.removePrefix(prefixDLQ)
  }

  return null
}
