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
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.events.data.services.serviceType
import io.infinitic.events.data.services.toJson
import io.infinitic.events.data.workflows.toJson
import io.infinitic.events.data.workflows.workflowType
import java.net.URI
import java.net.URLEncoder
import java.time.OffsetDateTime

fun Message.toServiceCloudEvent(publishedAt: MillisInstant, prefix: String): CloudEvent? =
    with(CloudEventContext.SERVICE) {
      when (val type = type()) {
        null -> null
        else -> CloudEventBuilder()
            .withId(messageId.toString())
            .withTime(time(publishedAt))
            .withType(type)
            .withSubject(subject())
            .withSource(source(prefix))
            .withDataContentType("application/json")
            .withoutDataSchema()
            .withData(dataBytes())
            .build()
      }
    }

fun Message.toWorkflowCloudEvent(publishedAt: MillisInstant, sourcePrefix: String): CloudEvent? =
    with(CloudEventContext.WORKFLOW) {
      when (val type = type()) {
        null -> null
        else -> CloudEventBuilder()
            .withId(messageId.toString())
            .withTime(time(publishedAt))
            .withType(type)
            .withSubject(subject())
            .withSource(source(sourcePrefix))
            .withDataContentType("application/json")
            .withoutDataSchema()
            .withData(dataBytes())
            .build()
      }
    }

enum class CloudEventContext {

  WORKFLOW {

    override fun Message.type(): String? = when (this) {
      is WorkflowCmdMessage -> workflowType()
      is WorkflowEngineMessage -> workflowType()
      is WorkflowEventMessage -> workflowType()
      else -> null
    }

    override fun Message.subject(): String = when (this) {
      is WorkflowCmdMessage -> workflowId
      is WorkflowEngineMessage -> workflowId
      is WorkflowEventMessage -> workflowId
      else -> thisShouldNotHappen()
    }.toString()

    override fun Message.source(prefix: String): URI = when (this) {
      is WorkflowCmdMessage -> workflowName
      is WorkflowEngineMessage -> workflowName
      is WorkflowEventMessage -> workflowName
      else -> thisShouldNotHappen()
    }.let {
      URI.create("$prefix/workflows/${it.encoded}")
    }

    override fun Message.dataBytes(): ByteArray = when (this) {
      is WorkflowCmdMessage -> toJson()
      is WorkflowEngineMessage -> toJson()
      is WorkflowEventMessage -> toJson()
      else -> thisShouldNotHappen()
    }.toString().toByteArray()

    override fun Message.time(publishedAt: MillisInstant): OffsetDateTime = when (this) {
      is WorkflowCmdMessage -> publishedAt
      is WorkflowEngineMessage -> emittedAt ?: publishedAt
      is WorkflowEventMessage -> publishedAt
      else -> thisShouldNotHappen()
    }.toOffsetDateTime()
  },

  SERVICE {

    override fun Message.type(): String? = when (this) {
      is ServiceExecutorMessage -> serviceType()
      is ServiceEventMessage -> serviceType()
      else -> null
    }

    override fun Message.subject(): String = when (this) {
      is ServiceExecutorMessage -> taskId
      is ServiceEventMessage -> taskId
      else -> thisShouldNotHappen()
    }.toString()

    override fun Message.source(prefix: String): URI = when (this) {
      is ServiceExecutorMessage -> when (isWorkflowTask()) {
        true -> "executor/${requester.workflowName.encoded}"
        false -> serviceName.encoded
      }

      is ServiceEventMessage -> when (isWorkflowTask()) {
        true -> "executor/${requester.workflowName.encoded}"
        false -> serviceName.encoded
      }

      else -> thisShouldNotHappen()
    }.let {
      URI.create("$prefix/services/$it")
    }

    override fun Message.dataBytes(): ByteArray = when (this) {
      is ServiceExecutorMessage -> toJson()
      is ServiceEventMessage -> toJson()
      else -> thisShouldNotHappen()
    }.toString().toByteArray()

    override fun Message.time(publishedAt: MillisInstant): OffsetDateTime =
        publishedAt.toOffsetDateTime()
  };

  companion object {
    private val Name?.encoded
      get() = URLEncoder.encode(toString(), Charsets.UTF_8)
  }


  abstract fun Message.time(publishedAt: MillisInstant): OffsetDateTime
  abstract fun Message.type(): String?
  abstract fun Message.subject(): String
  abstract fun Message.source(prefix: String): URI
  abstract fun Message.dataBytes(): ByteArray

}
