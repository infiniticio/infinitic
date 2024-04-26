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
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventEnvelope
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagEnvelope
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.ServiceTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.transport.WorkflowTopic
import io.infinitic.common.workflows.engine.messages.WorkflowCmdEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.shade.org.apache.commons.lang.StringEscapeUtils
import kotlin.reflect.KClass

private const val SEPARATOR = ":"

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
  WorkflowTaskExecutorTopic, DelayedWorkflowTaskExecutorTopic -> "workflow-task-executor"
  WorkflowTaskEventsTopic -> "workflow-task-events"
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
internal fun Topic<*>.name(entity: String?) =
    prefix() + (entity?.let { "$SEPARATOR${StringEscapeUtils.escapeJava(it)}" } ?: "")

/**
 * Returns the name of the Dead Letter Queue Topic based on topic type and the entity name
 *
 * @param entity The optional entity name (service name or workflow name).
 * @return The name of the Topic with optional entity name.
 */
internal fun Topic<*>.nameDLQ(entity: String) =
    "${prefixDLQ()}$SEPARATOR${StringEscapeUtils.escapeJava(entity)}"


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
    val prefix = serviceTopic.prefix() + SEPARATOR
    if (topicName.startsWith(prefix)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefix),
    )

    val prefixDLQ = serviceTopic.prefixDLQ() + SEPARATOR
    if (topicName.startsWith(prefixDLQ)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefixDLQ),
    )
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
    val prefix = workflowTopic.prefix() + SEPARATOR
    if (topicName.startsWith(prefix)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefix),
    )

    val prefixDLQ = workflowTopic.prefixDLQ() + SEPARATOR
    if (topicName.startsWith(prefixDLQ)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefixDLQ),
    )
  }

  return null
}

/**
 * Returns the envelope class associated with a topic type.
 *
 * @return The envelope class that is associated with the topic.
 */
@Suppress("UNCHECKED_CAST")
internal val <S : Message> Topic<S>.envelopeClass: KClass<Envelope<out S>>
  get() = when (this) {
    NamingTopic -> thisShouldNotHappen()
    ClientTopic -> ClientEnvelope::class
    WorkflowTagTopic -> WorkflowTagEnvelope::class
    WorkflowCmdTopic -> WorkflowCmdEnvelope::class
    WorkflowEngineTopic, DelayedWorkflowEngineTopic -> WorkflowEngineEnvelope::class
    WorkflowEventsTopic -> WorkflowEventEnvelope::class
    WorkflowTaskExecutorTopic, DelayedWorkflowTaskExecutorTopic -> ServiceExecutorEnvelope::class
    WorkflowTaskEventsTopic -> ServiceEventEnvelope::class
    ServiceTagTopic -> ServiceTagEnvelope::class
    ServiceExecutorTopic, DelayedServiceExecutorTopic -> ServiceExecutorEnvelope::class
    ServiceEventsTopic -> ServiceEventEnvelope::class
  } as KClass<Envelope<out S>>

@Suppress("UNCHECKED_CAST")
fun <S : Message> Topic<S>.envelope(message: S) = when (this) {
  NamingTopic -> thisShouldNotHappen()
  ClientTopic -> ClientEnvelope.from(message as ClientMessage)
  WorkflowTagTopic -> WorkflowTagEnvelope.from(message as WorkflowTagMessage)
  WorkflowCmdTopic -> WorkflowCmdEnvelope.from(message as WorkflowCmdMessage)
  WorkflowEngineTopic, DelayedWorkflowEngineTopic -> WorkflowEngineEnvelope.from(message as WorkflowEngineMessage)
  WorkflowEventsTopic -> WorkflowEventEnvelope.from(message as WorkflowEventMessage)
  WorkflowTaskExecutorTopic, DelayedWorkflowTaskExecutorTopic ->
    ServiceExecutorEnvelope.from(message as ServiceExecutorMessage)

  WorkflowTaskEventsTopic -> ServiceEventEnvelope.from(message as ServiceEventMessage)
  ServiceTagTopic -> ServiceTagEnvelope.from(message as ServiceTagMessage)
  ServiceExecutorTopic, DelayedServiceExecutorTopic -> ServiceExecutorEnvelope.from(message as ServiceExecutorMessage)
  ServiceEventsTopic -> ServiceEventEnvelope.from(message as ServiceEventMessage)
} as Envelope<out S>

/**
 * Returns a boolean indicating if the topic should be created when producing a message to this topic
 */
internal val <S : Message> Topic<S>.initWhenProducing: Boolean
  get() = when (this) {
    ClientTopic -> false
    else -> true
  }
