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
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
sealed class WorkflowTagMessage : Message {
  override val messageId = MessageId()
  abstract val workflowTag: WorkflowTag
  abstract val workflowName: WorkflowName
  abstract val emittedAt: MillisInstant?

  override fun envelope() = WorkflowTagEnvelope.from(this)

  override fun key() = workflowTag.toString()

  override fun entity() = workflowName.toString()
}

/**
 * This message is a command to send a signal to all workflow instances with the provided tag.
 *
 * @param workflowName the name of the workflows to send the signal to
 * @param workflowTag the tag of the workflows to send the signal to
 * @param channelName the name of the channel to send the signal to
 * @param signalId the id of the signal to send
 * @param signalData the data of the signal to send
 * @param channelTypes the types of the channel to send the signal to
 * @param parentWorkflowId the id of the workflow that emitted this command
 * @param emitterName the name of the client that emitted this command
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
  @AvroName("emitterWorkflowId") var parentWorkflowId: WorkflowId?,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?

) : WorkflowTagMessage()

/**
 * This message is a command to cancel all workflow instances with the provided tag.
 *
 * @param workflowName the name of the workflows to cancel
 * @param workflowTag the tag of the workflows to cancel
 * @param reason the reason of the cancellation
 * @param emitterWorkflowId the id of the workflow that emitted this command
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CancelWorkflowByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroNamespace("io.infinitic.workflows.data") val reason: WorkflowCancellationReason,
  var emitterWorkflowId: WorkflowId?,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to retry the workflow task of all workflow instances with the provided tag.
 *
 * @param workflowName the name of the workflow instances to retry the workflow task of
 * @param workflowTag the tag of the workflow instances to retry the workflow task of
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryWorkflowTaskByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to retry the tasks of all workflow instances with the provided tag.
 *
 * @param workflowName the name of the workflow instances to retry the tasks of
 * @param workflowTag the tag of the workflow instances to retry the tasks of
 * @param taskId the id of the task to retry
 * @param taskStatus the status of the task to retry
 * @param serviceName the name of the service of the task to retry
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryTasksByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val taskId: TaskId?,
  val taskStatus: DeferredStatus?,
  @SerialName("taskName") val serviceName: ServiceName?,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to complete all timers of all workflow instances with the provided tag.
 *
 * @param workflowName the name of the workflow instances to complete the timers of
 * @param workflowTag the tag of the workflow instances to complete the timers of
 * @param workflowMethodId the id of the method run to complete the timers of
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CompleteTimersByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId?,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to tell the tag engine that a workflow with the provided tag is running
 *
 * @param workflowName the name of the new workflow running
 * @param workflowTag the tag of the new workflow running
 * @param workflowId the id of the new workflow running
 * @param emitterName the name of the client that emitted this command
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
 *
 * @param workflowName the name of the workflow that stopped running
 * @param workflowTag the tag of the workflow that stopped running
 * @param workflowId the id of the workflow that stopped running
 * @param emitterName the name of the client that emitted this command
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
 *
 * @param workflowName the name of the workflows to get the ids of
 * @param workflowTag the tag of the workflows to get the ids of
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class GetWorkflowIdsByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()

/**
 * This message is a command to dispatch a new workflow instance,
 * but only after having checked if it is not already running.
 *
 * @param workflowName the name of the workflow to dispatch
 * @param workflowTag the tag of the workflow to dispatch
 * @param workflowId the id of the workflow to dispatch
 * @param methodName the name of the method to dispatch
 * @param methodParameters the parameters of the method to dispatch
 * @param methodParameterTypes the types of the parameters of the method to dispatch
 * @param methodTimeout the timeout of the method to dispatch
 * @param workflowTags the tags of the workflow to dispatch
 * @param workflowMeta the meta of the workflow to dispatch
 * @param parentWorkflowName the name of the parent workflow that triggered this command
 * @param parentWorkflowId the id of the parent workflow that triggered this command
 * @param parentWorkflowMethodId the id of the parent method run that triggered this command
 * @param clientWaiting whether the client is waiting for the result of the workflow
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class DispatchWorkflowByCustomId(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  val workflowId: WorkflowId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  @AvroDefault(Avro.NULL) val methodTimeout: MillisDuration? = null,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  var parentWorkflowName: WorkflowName?,
  var parentWorkflowId: WorkflowId?,
  @AvroName("parentMethodRunId") var parentWorkflowMethodId: WorkflowMethodId?,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage() {
  init {
    require(workflowTag.isCustomId()) { "workflowTag must be a custom id" }
  }
}

/**
 * This message is a command to dispatch a method on running workflow instances with the provided tag.
 *
 * @param workflowName the name of the targeted running workflow instances
 * @param workflowTag the tag of the targeted running workflow instances
 * @param workflowMethodId the Id of the method to dispatch
 * @param methodName the name of the method to dispatch
 * @param methodParameters the parameters of the method to dispatch
 * @param methodParameterTypes the types of the parameters of the method to dispatch
 * @param methodTimeout the timeout of the method to dispatch
 * @param clientWaiting whether the client is waiting for the result of the workflow
 * @param emitterName the name of the client that emitted this command
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
@AvroName("DispatchMethodByTag")
data class DispatchMethodByTag(
  override val workflowName: WorkflowName,
  override val workflowTag: WorkflowTag,
  var parentWorkflowId: WorkflowId?,
  var parentWorkflowName: WorkflowName?,
  @AvroName("parentMethodRunId") var parentWorkflowMethodId: WorkflowMethodId?,
  @AvroName("methodRunId")
  val workflowMethodId: WorkflowMethodId,
  val methodName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodParameters,
  @AvroDefault(Avro.NULL) val methodTimeout: MillisDuration? = null,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override val emittedAt: MillisInstant?
) : WorkflowTagMessage()
