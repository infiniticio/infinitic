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
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagEnvelope
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowExecutorTopic
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.ServiceTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.WorkflowTopic
import io.infinitic.common.workflows.engine.messages.WorkflowCmdEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import io.infinitic.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.shade.org.apache.commons.lang.StringEscapeUtils
import kotlin.reflect.KClass

private const val SEPARATOR = ":"


internal val Topic<*>.prefixDLQ get() = "$prefix-dlq"

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
 * Returns the name of the Topic based on topic type and the entity name
 *
 * @param entity The optional entity name (service name or workflow name).
 * @return The name of the Topic with optional entity name.
 */
internal fun Topic<*>.name(entity: String?) =
    prefix + (entity?.let { "$SEPARATOR${StringEscapeUtils.escapeJava(it)}" } ?: "")

/**
 * Returns the name of the Dead Letter Queue Topic based on topic type and the entity name
 *
 * @param entity The optional entity name (service name or workflow name).
 * @return The name of the Topic with optional entity name.
 */
internal fun Topic<*>.nameDLQ(entity: String) =
    "$prefixDLQ$SEPARATOR${StringEscapeUtils.escapeJava(entity)}"


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
    val prefix = serviceTopic.prefix + SEPARATOR
    if (topicName.startsWith(prefix)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefix),
    )

    val prefixDLQ = serviceTopic.prefixDLQ + SEPARATOR
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
    val prefix = workflowTopic.prefix + SEPARATOR
    if (topicName.startsWith(prefix)) return StringEscapeUtils.unescapeJava(
        topicName.removePrefix(prefix),
    )

    val prefixDLQ = workflowTopic.prefixDLQ + SEPARATOR
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
    WorkflowTagEngineTopic -> WorkflowTagEnvelope::class
    WorkflowStateCmdTopic -> WorkflowCmdEnvelope::class
    WorkflowStateEngineTopic, WorkflowStateTimerTopic -> WorkflowEngineEnvelope::class
    WorkflowStateEventTopic -> WorkflowEventEnvelope::class
    WorkflowExecutorTopic, RetryWorkflowExecutorTopic -> ServiceExecutorEnvelope::class
    WorkflowExecutorEventTopic -> ServiceEventEnvelope::class
    ServiceTagEngineTopic -> ServiceTagEnvelope::class
    ServiceExecutorTopic, RetryServiceExecutorTopic -> ServiceExecutorEnvelope::class
    ServiceExecutorEventTopic -> ServiceEventEnvelope::class
  } as KClass<Envelope<out S>>

@Suppress("UNCHECKED_CAST")
internal fun <S : Message> Topic<S>.envelope(message: S) = when (this) {
  NamingTopic -> thisShouldNotHappen()
  ClientTopic -> ClientEnvelope.from(message as ClientMessage)
  WorkflowTagEngineTopic -> WorkflowTagEnvelope.from(message as WorkflowTagEngineMessage)
  WorkflowStateCmdTopic -> WorkflowCmdEnvelope.from(message as WorkflowCmdMessage)
  WorkflowStateEngineTopic, WorkflowStateTimerTopic -> WorkflowEngineEnvelope.from(message as WorkflowStateEngineMessage)
  WorkflowStateEventTopic -> WorkflowEventEnvelope.from(message as WorkflowEventMessage)
  WorkflowExecutorTopic, RetryWorkflowExecutorTopic ->
    ServiceExecutorEnvelope.from(message as ServiceExecutorMessage)

  WorkflowExecutorEventTopic -> ServiceEventEnvelope.from(message as ServiceExecutorEventMessage)
  ServiceTagEngineTopic -> ServiceTagEnvelope.from(message as ServiceTagMessage)
  ServiceExecutorTopic, RetryServiceExecutorTopic -> ServiceExecutorEnvelope.from(message as ServiceExecutorMessage)
  ServiceExecutorEventTopic -> ServiceEventEnvelope.from(message as ServiceExecutorEventMessage)
} as Envelope<out S>

/**
 * Returns a boolean indicating if the topic should be created when producing a message to this topic
 */
internal val <S : Message> Topic<S>.initWhenProducing: Boolean
  get() = when (this) {
    ClientTopic -> false
    else -> true
  }
