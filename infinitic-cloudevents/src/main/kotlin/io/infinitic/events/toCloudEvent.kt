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

package io.infinitic.events

import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.workflowName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.workflows.engine.messages.WorkflowStateCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import io.infinitic.events.data.services.serviceType
import io.infinitic.events.data.services.toJson
import io.infinitic.events.data.workflows.toJson
import io.infinitic.events.data.workflows.workflowType
import java.net.URI
import java.net.URLEncoder

private fun <S : Message> S.isRedundantIn(topic: Topic<*>) = when (topic) {
  is WorkflowStateEngineTopic -> this is WorkflowStateCmdMessage
  else -> false
}

fun <S : Message> S.toCloudEvent(
  topic: Topic<*>,
  publishedAt: MillisInstant,
  prefix: String
): CloudEvent? = if (isRedundantIn(topic)) null else
  when (val type = type()) {
    null -> null
    else -> CloudEventBuilder()
        .withId(messageId.toString())
        .withTime(publishedAt.toOffsetDateTime())
        .withType(type)
        .withSubject(subject())
        .withSource(source(prefix))
        .withDataContentType("application/json")
        .withoutDataSchema()
        .withData(dataBytes())
        .build()
  }

private fun Message.type(): String? = when (this) {
  is WorkflowStateCmdMessage -> workflowType()
  is WorkflowStateEngineMessage -> workflowType()
  is WorkflowStateEventMessage -> workflowType()
  is ServiceExecutorMessage -> serviceType()
  is ServiceExecutorEventMessage -> serviceType()
  else -> null
}

private fun Message.subject(): String = when (this) {
  is WorkflowStateCmdMessage -> workflowId
  is WorkflowStateEngineMessage -> workflowId
  is WorkflowStateEventMessage -> workflowId
  is ServiceExecutorMessage -> taskId
  is ServiceExecutorEventMessage -> taskId
  else -> thisShouldNotHappen()
}.toString()

private fun Message.source(prefix: String): URI = when (this) {
  is WorkflowStateCmdMessage -> "workflows/stateEngine/${workflowName.encoded}"
  is WorkflowStateEngineMessage -> "workflows/stateEngine/${workflowName.encoded}"
  is WorkflowStateEventMessage -> "workflows/stateEngine/${workflowName.encoded}"
  is ServiceExecutorMessage -> when (isWorkflowTask()) {
    true -> "workflows/executor/${requester.workflowName.encoded}"
    false -> "services/executor/${serviceName.encoded}"
  }

  is ServiceExecutorEventMessage -> when (isWorkflowTask()) {
    true -> "workflows/executor/${requester.workflowName.encoded}"
    false -> "services/executor/${serviceName.encoded}"
  }

  else -> thisShouldNotHappen()
}.let {
  URI.create("$prefix/$it")
}

private fun Message.dataBytes(): ByteArray = when (this) {
  is WorkflowStateCmdMessage -> toJson()
  is WorkflowStateEngineMessage -> toJson()
  is WorkflowStateEventMessage -> toJson()
  is ServiceExecutorMessage -> toJson()
  is ServiceExecutorEventMessage -> toJson()
  else -> thisShouldNotHappen()
}.toString().toByteArray()

internal val Name?.encoded: String
  get() = URLEncoder.encode(toString(), Charsets.UTF_8)
