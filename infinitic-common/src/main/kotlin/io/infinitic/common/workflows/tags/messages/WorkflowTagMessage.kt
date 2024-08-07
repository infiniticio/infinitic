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
package io.infinitic.common.workflows.tags.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface WorkflowTagCmdMessage {
  val requester: Requester?
}

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
sealed class WorkflowTagMessage : Message {
  override val messageId = MessageId()
  abstract val workflowTag: WorkflowTag
  abstract val workflowName: WorkflowName
  abstract val emittedAt: MillisInstant?

  override fun key() = workflowTag.toString()

  override fun entity() = workflowName.toString()
}

/**
 * This message is a command to send a signal to all workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class SendSignalByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val channelName: ChannelName,
  @AvroName("channelSignalId") val signalId: SignalId,
  @AvroName("channelSignal") val signalData: SignalData,
  @AvroName("channelSignalTypes") val channelTypes: Set<ChannelType>,
  @Deprecated("Not used since version 0.13.0") @AvroName("emitterWorkflowId") var parentWorkflowId: WorkflowId? = null,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage

/**
 * This message is a command to cancel all workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CancelWorkflowByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroNamespace("io.infinitic.workflows.data") val reason: WorkflowCancellationReason,
  @Deprecated("Not used since version 0.13.0") var emitterWorkflowId: WorkflowId? = null,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage {
  init {
    // this is used only to handle previous messages that are still on <0.13 version
    // in topics or in bufferedMessages of a workflow state
    requester = requester ?: when (emitterWorkflowId) {
      null -> ClientRequester(clientName = ClientName.from(emitterName))
      else -> WorkflowRequester(
          workflowId = emitterWorkflowId!!,
          workflowName = WorkflowName("undefined"),
          workflowVersion = null,
          workflowMethodName = MethodName("undefined"),
          workflowMethodId = WorkflowMethodId("undefined"),
      )
    }
  }
}

/**
 * This message is a command to retry the workflow task of all workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryWorkflowTaskByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage

/**
 * This message is a command to retry the tasks of all workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryTasksByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val taskId: TaskId?,
  val taskStatus: DeferredStatus?,
  @SerialName("taskName") val serviceName: ServiceName?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage

/**
 * This message is a command to complete all timers of all workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CompleteTimersByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage

/**
 * This message is a command to tell the tag engine that a workflow with the provided tag is running
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class AddTagToWorkflow(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to tell the tag engine that a workflow with the provided tag is not running anymore
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RemoveTagFromWorkflow(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to request the tag engine to send back the ids of all workflows with the provided tag
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class GetWorkflowIdsByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
) : WorkflowTagMessage()

/**
 * This message is a command to dispatch a new workflow instance,
 * but only after having checked if it is not already running.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class DispatchWorkflowByCustomId(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val workflowId: WorkflowId,
  val methodName: MethodName,
  val methodParameters: MethodArgs,
  val methodParameterTypes: MethodParameterTypes?,
  @AvroDefault(Avro.NULL) val methodTimeout: MillisDuration? = null,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowName: WorkflowName? = null,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowId: WorkflowId? = null,
  @Deprecated("Not used since version 0.13.0") val parentMethodRunId: WorkflowMethodId? = null,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage(), WorkflowTagCmdMessage {
  init {
    require(workflowTag.isCustomId()) { "workflowTag must be a custom id" }

    // this is used only to handle previous messages that are still on <0.13 version
    // in topics or in bufferedMessages of a workflow state
    requester = requester ?: when (parentWorkflowId) {
      null -> ClientRequester(clientName = ClientName.from(emitterName))
      else -> WorkflowRequester(
          workflowId = parentWorkflowId,
          workflowName = parentWorkflowName ?: WorkflowName("undefined"),
          workflowVersion = null,
          workflowMethodName = MethodName("undefined"),
          workflowMethodId = parentMethodRunId ?: WorkflowMethodId("undefined"),
      )
    }
  }
}

/**
 * This message is a command to dispatch a method on running workflow instances with the provided tag.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
@AvroName("DispatchMethodByTag")
data class DispatchMethodByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowId: WorkflowId? = null,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowName: WorkflowName? = null,
  @Deprecated("Not used since version 0.13.0") val parentMethodRunId: WorkflowMethodId? = null,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  val clientWaiting: Boolean,
  val methodName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodArgs,
  @AvroDefault(Avro.NULL) val methodTimeout: MillisDuration? = null,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?,
  override val emitterName: EmitterName,
) : WorkflowTagMessage(), WorkflowTagCmdMessage {
  init {
    // this is used only to handle previous messages that are still on <0.13 version
    // in topics or in bufferedMessages of a workflow state
    requester = requester ?: when (parentWorkflowId) {
      null -> ClientRequester(clientName = ClientName.from(emitterName))
      else -> WorkflowRequester(
          workflowId = parentWorkflowId,
          workflowName = parentWorkflowName ?: WorkflowName("undefined"),
          workflowVersion = null,
          workflowMethodName = MethodName("undefined"),
          workflowMethodId = parentMethodRunId ?: WorkflowMethodId("undefined"),
      )
    }
  }
}
