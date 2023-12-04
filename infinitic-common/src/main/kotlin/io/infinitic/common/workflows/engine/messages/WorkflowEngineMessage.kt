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
package io.infinitic.common.workflows.engine.messages

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodTimedOutError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface WorkflowEvent

interface MethodEvent : WorkflowEvent {
  val methodRunId: MethodRunId
}

interface TaskEvent : MethodEvent {
  fun taskId(): TaskId

  fun serviceName(): ServiceName
}

@Serializable
sealed class WorkflowEngineMessage : Message {
  val messageId: MessageId = MessageId()
  abstract val emitterName: ClientName
  abstract val workflowId: WorkflowId
  abstract val workflowName: WorkflowName

  override fun envelope() = WorkflowEngineEnvelope.from(this)

  fun isWorkflowTaskEvent() =
      (this is TaskEvent) && this.serviceName() == ServiceName(WorkflowTask::class.java.name)
}

/**
 * This message is a command for dispatching a new workflow.
 *
 * @param workflowName Name of the workflow to dispatch
 * @param workflowId Id of the workflow to dispatch
 * @param methodName Name of the method to dispatch
 * @param methodParameters Parameters of the method to dispatch
 * @param methodParameterTypes Parameter types of the method to dispatch
 * @param workflowTags Tags of the workflow to dispatch
 * @param workflowMeta Meta of the workflow to dispatch
 * @param parentWorkflowId Id of the workflow that triggered the command
 * @param parentWorkflowName Name of the workflow that triggered the command
 * @param parentMethodRunId Id of the method that triggered the command
 * @param clientWaiting if a client is waiting for the workflow to complete
 * @param emitterName Name of the emitter
 */

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("DispatchWorkflow")
data class DispatchNewWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  var parentWorkflowName: WorkflowName?,
  var parentWorkflowId: WorkflowId?,
  var parentMethodRunId: MethodRunId?,
  val clientWaiting: Boolean,
  override val emitterName: ClientName
) : WorkflowEngineMessage()

/**
 * This message is a command to dispatch a new method for a running workflow.
 * If this request was triggered from another workflow, then
 * [parentWorkflowName], [parentWorkflowId] and [parentMethodRunId] describe it
 *
 * @param workflowName Name of the running workflow
 * @param workflowId Id of the running workflow
 * @param methodRunId Id of the running method
 * @param methodName Name of the method to dispatch
 * @param methodParameters Parameters of the method to dispatch
 * @param methodParameterTypes Parameter types of the method to dispatch
 * @param parentWorkflowId Id of the workflow that triggered the command
 * @param parentWorkflowName Name of the workflow that triggered the command
 * @param parentMethodRunId Id of the method that triggered the command
 * @param clientWaiting if a client is waiting for the method to complete
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("DispatchMethod")
data class DispatchMethodOnRunningWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodRunId: MethodRunId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  var parentWorkflowId: WorkflowId?,
  var parentWorkflowName: WorkflowName?,
  var parentMethodRunId: MethodRunId?,
  val clientWaiting: Boolean,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), WorkflowEvent

/**
 * This message is a command to retry the workflow task of a running workflow.
 *
 * @param workflowName Name of the workflow for which we retry the workflow task
 * @param workflowId Id of the workflow for which we retry the workflow task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RetryWorkflowTask(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: ClientName
) : WorkflowEngineMessage()

/**
 * This message is a command to retry some task of a running workflow.
 * The tasks to retry are selected by their id, status or service name.
 *
 * @param workflowName Name of the workflow for which we retry the tasks
 * @param workflowId Id of the workflow for which we retry the tasks
 * @param taskId Select the task to retry by its id (if any)
 * @param taskStatus Select the task to retry by its status (if any)
 * @param serviceName Select the task to retry by its name (if any)
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RetryTasks(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val taskId: TaskId?,
  val taskStatus: DeferredStatus?,
  @SerialName("taskName") val serviceName: ServiceName?,
  override val emitterName: ClientName
) : WorkflowEngineMessage()

/**
 * This message is a command to complete timers of a running workflow.
 *
 * @param workflowName Name of the workflow for which we complete the timers
 * @param workflowId Id of the workflow for which we complete the timers
 * @param methodRunId Id of the method for which we complete the timers (if any)
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CompleteTimers(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodRunId: MethodRunId?,
  override val emitterName: ClientName
) : WorkflowEngineMessage()

/**
 * This message is a command to tell a workflow's method run that a client is waiting for its output
 *
 * @param workflowName Name of the workflow to wait for
 * @param workflowId Id of the workflow to wait for
 * @param methodRunId Id of the method to wait for
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WaitWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodRunId: MethodRunId,
  override val emitterName: ClientName
) : WorkflowEngineMessage()

/**
 * This message is a command to cancel a running workflow
 *
 * @param workflowName Name of the workflow to cancel
 * @param workflowId Id of the workflow to cancel
 * @param methodRunId Id of the method to cancel (if any)
 * @param reason Reason of the cancellation
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CancelWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodRunId: MethodRunId?,
  @AvroNamespace("io.infinitic.workflows.data") val reason: WorkflowCancellationReason,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), WorkflowEvent

/**
 * This message is a command to manually complete a running workflow
 *
 * @param workflowName Name of the workflow to complete
 * @param workflowId Id of the workflow to complete
 * @param workflowReturnValue Provided return value of the workflow
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CompleteWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val workflowReturnValue: ReturnValue,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), WorkflowEvent

/**
 * This message is a command to send a signal to a running workflow
 *
 * @param workflowName Name of the workflow to send the signal to
 * @param workflowId Id of the workflow to send the signal to
 * @param channelName Name of the channel to send the signal to
 * @param signalId Id of the signal to send (to manage idempotency)
 * @param signalData Data of the signal to send
 * @param channelTypes Types of the signal to send
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class SendSignal(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val channelName: ChannelName,
  @AvroName("channelSignalId") val signalId: SignalId,
  @AvroName("channelSignal") val signalData: SignalData,
  @AvroName("channelSignalTypes") val channelTypes: Set<ChannelType>,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), WorkflowEvent

/**
 * This message is an event telling a running workflow that another workflow's method is unknown.
 *
 * This event is received when a workflow waits for the completion of
 * another workflow method that is already completed or does not exist.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param childMethodUnknownError Error describing the unknown method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodUnknown(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  @SerialName("childUnknownWorkflowError")
  val childMethodUnknownError: MethodUnknownError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has been canceled.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param childMethodCanceledError Error describing the canceled method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodCanceled(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  @SerialName("childCanceledWorkflowError")
  val childMethodCanceledError: MethodCanceledError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has failed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param childMethodFailedError Error describing the failed method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodFailed(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  @SerialName("childFailedWorkflowError")
  val childMethodFailedError: MethodFailedError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has timed out.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param childMethodTimedOutError Error describing the timed out method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodTimedOut(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  val childMethodTimedOutError: MethodTimedOutError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has completed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param childWorkflowReturnValue Return value of the completed method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodCompleted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  val childWorkflowReturnValue: WorkflowReturnValue,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent

/**
 * This message is an event telling a workflow that a task was canceled.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param taskCanceledError Error describing the canceled task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskCanceled(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  @SerialName("canceledTaskError")
  val taskCanceledError: TaskCanceledError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskCanceledError.taskId

  override fun serviceName() = taskCanceledError.serviceName
}

/**
 * This message is an event telling a workflow that a task has failed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param taskFailedError Error describing the failed task
 * @param deferredError if the task is a workflow task, and the failure is due to a deferred error
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskFailed(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  @SerialName("failedTaskError")
  val taskFailedError: TaskFailedError,
  val deferredError: DeferredError?,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskFailedError.taskId

  override fun serviceName() = taskFailedError.serviceName
}

/**
 * This message is an event telling a workflow that a task has timed out.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param taskTimedOutError Error describing the timed out task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskTimedOut(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  val taskTimedOutError: TaskTimedOutError,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskTimedOutError.taskId

  override fun serviceName() = taskTimedOutError.serviceName
}

/**
 * This message is an event telling a workflow that a task has completed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param methodRunId Id of the method receiving the event
 * @param taskReturnValue Return value of the completed task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskCompleted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  val taskReturnValue: TaskReturnValue,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskReturnValue.taskId

  override fun serviceName() = taskReturnValue.serviceName
}

/**
 * This message is an event telling a running workflow that a timer has completed
 *
 * @param workflowName Name of the workflow to complete the timer for
 * @param workflowId Id of the workflow to complete the timer for
 * @param methodRunId Id of the method to complete the timer for
 * @param timerId Id of the timer to complete
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TimerCompleted(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val methodRunId: MethodRunId,
  val timerId: TimerId,
  override val emitterName: ClientName
) : WorkflowEngineMessage(), MethodEvent
