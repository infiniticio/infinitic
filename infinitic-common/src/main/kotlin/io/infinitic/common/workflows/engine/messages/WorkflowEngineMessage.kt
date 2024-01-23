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

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.Version
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.MethodCanceledError
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.executors.errors.MethodUnknownError
import io.infinitic.common.tasks.executors.errors.TaskCanceledError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.errors.WorkflowMethodTimedOutError
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethod
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.events.WorkflowMethodStartedEvent
import io.infinitic.common.workflows.engine.events.WorkflowStartedEvent
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.currentVersion
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface WorkflowInternalEvent

interface WorkflowMethodEvent : WorkflowInternalEvent {
  val workflowMethodId: WorkflowMethodId
}

interface TaskEvent : WorkflowMethodEvent {
  fun taskId(): TaskId

  fun serviceName(): ServiceName
}

@Serializable
sealed class WorkflowEngineMessage : Message {
  @Suppress("RedundantNullableReturnType")
  @AvroDefault(Avro.NULL)
  val version: Version? = Version(currentVersion)
  override val messageId: MessageId = MessageId()
  abstract val workflowId: WorkflowId
  abstract val workflowName: WorkflowName

  // emittedAt has been introduced in 0.13.0
  // This property represents the time this event is supposed to have been emitted:
  // * if published by a client: the publishing time (got from transport)
  // * the published time (from transport) of the event that triggered the workflow task that triggered this event
  abstract var emittedAt: MillisInstant?

  override fun key() = workflowId.toString()

  override fun entity() = workflowName.toString()

  fun isWorkflowTaskEvent() =
      (this is TaskEvent) && this.serviceName() == ServiceName(WorkflowTask::class.java.name)
}

/**
 * This message retries the workflow task of a running workflow.
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
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage()

/**
 * This message retries some task of a running workflow.
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
  val taskId: TaskId?,
  val taskStatus: DeferredStatus?,
  @SerialName("taskName") val serviceName: ServiceName?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage()

/**
 * This message tells a workflow's method that a new client is waiting for its output
 *
 * @param workflowName Name of the workflow to wait for
 * @param workflowId Id of the workflow to wait for
 * @param workflowMethodId Id of the method to wait for
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WaitWorkflow(
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage()

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
 * @param parentWorkflowMethodId Id of the method that triggered the command
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
  val parentWorkflowName: WorkflowName?,
  val parentWorkflowId: WorkflowId?,
  @AvroName("parentMethodRunId") var parentWorkflowMethodId: WorkflowMethodId?,
  @AvroDefault(Avro.NULL) val workflowTaskId: TaskId? = null,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage() {

  fun workflowMethod() = WorkflowMethod(
      workflowMethodId = WorkflowMethodId.from(workflowId),
      waitingClients = waitingClients(),
      parentWorkflowId = parentWorkflowId,
      parentWorkflowName = parentWorkflowName,
      parentWorkflowMethodId = parentWorkflowMethodId,
      parentClientName = parentClientName(),
      methodName = methodName,
      methodParameterTypes = methodParameterTypes,
      methodParameters = methodParameters,
      workflowTaskIndexAtStart = WorkflowTaskIndex(0),
      propertiesNameHashAtStart = mapOf(),
  )


  fun state() = WorkflowState(
      lastMessageId = messageId,
      workflowId = workflowId,
      workflowName = workflowName,
      workflowVersion = null,
      workflowTags = workflowTags,
      workflowMeta = workflowMeta,
      workflowMethods = mutableListOf(workflowMethod()),
      workflowTaskIndex = WorkflowTaskIndex(1),
      runningWorkflowTaskId = workflowTaskId ?: thisShouldNotHappen(),
      runningWorkflowTaskInstant = emittedAt ?: thisShouldNotHappen(),
      runningWorkflowMethodId = WorkflowMethodId.from(workflowId),
      positionInRunningWorkflowMethod = PositionInWorkflowMethod(),
  )

  fun workflowStartedEvent(emitterName: EmitterName) = WorkflowStartedEvent(
      workflowName = workflowName,
      workflowId = workflowId,
      emitterName = emitterName,
      workflowTags = workflowTags,
      workflowMeta = workflowMeta,
  )

  fun workflowMethodStartedEvent(emitterName: EmitterName) = WorkflowMethodStartedEvent(
      workflowName = workflowName,
      workflowId = workflowId,
      emitterName = emitterName,
      workflowTags = workflowTags,
      workflowMeta = workflowMeta,
      workflowMethodId = WorkflowMethodId.from(workflowId),
      parentWorkflowName = parentWorkflowName,
      parentWorkflowId = parentWorkflowId,
      parentWorkflowMethodId = parentWorkflowMethodId,
      parentClientName = parentClientName(),
      waitingClients = if (clientWaiting) setOf(parentClientName()!!) else setOf(),
  )

  fun parentClientName(): ClientName? = when (parentWorkflowId) {
    null -> ClientName.from(emitterName)
    else -> null
  }

  fun waitingClients() = when (clientWaiting) {
    true -> mutableSetOf(parentClientName()!!)
    false -> mutableSetOf()
  }
}


/**
 * This message is a command to dispatch a new method for a running workflow.
 * If this request was triggered from another workflow, then
 * [parentWorkflowName], [parentWorkflowId] and [parentWorkflowMethodId] describe it
 *
 * @param workflowName Name of the running workflow
 * @param workflowId Id of the running workflow
 * @param workflowMethodId Id of the running method
 * @param methodName Name of the method to dispatch
 * @param methodParameters Parameters of the method to dispatch
 * @param methodParameterTypes Parameter types of the method to dispatch
 * @param parentWorkflowId Id of the workflow that triggered the command
 * @param parentWorkflowName Name of the workflow that triggered the command
 * @param parentWorkflowMethodId Id of the method that triggered the command
 * @param clientWaiting if a client is waiting for the method to complete
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("DispatchMethod")
data class DispatchMethodWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  var parentWorkflowId: WorkflowId?,
  var parentWorkflowName: WorkflowName?,
  @AvroName("parentMethodRunId") var parentWorkflowMethodId: WorkflowMethodId?,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowInternalEvent

val DispatchMethodWorkflow.parentClientName
  get() = when (parentWorkflowId) {
    null -> ClientName.from(emitterName)
    else -> null
  }


/**
 * This message is a command to complete timers of a running workflow.
 *
 * @param workflowName Name of the workflow for which we complete the timers
 * @param workflowId Id of the workflow for which we complete the timers
 * @param workflowMethodId Id of the method for which we complete the timers (if any)
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CompleteTimers(
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage()

/**
 * This message is a command to cancel a running workflow
 *
 * @param workflowName Name of the workflow to cancel
 * @param workflowId Id of the workflow to cancel
 * @param workflowMethodId Id of the method to cancel (if any)
 * @param cancellationReason Reason of the cancellation
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CancelWorkflow(
  @AvroNamespace("io.infinitic.workflows.data")
  @AvroName("reason") val cancellationReason: WorkflowCancellationReason,
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowInternalEvent

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
  val workflowReturnValue: ReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowInternalEvent

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
  val channelName: ChannelName,
  @AvroName("channelSignalId") val signalId: SignalId,
  @AvroName("channelSignal") val signalData: SignalData,
  @AvroName("channelSignalTypes") val channelTypes: Set<ChannelType>,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowInternalEvent

/**
 * This message is an event telling a running workflow that another workflow's method is unknown.
 *
 * This event is received when a workflow waits for the completion of
 * another workflow method that is already completed or does not exist.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param childMethodUnknownError Error describing the unknown method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodUnknown(
  @SerialName("childUnknownWorkflowError")
  val childMethodUnknownError: MethodUnknownError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has been canceled.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param childMethodCanceledError Error describing the canceled method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodCanceled(
  @SerialName("childCanceledWorkflowError")
  val childMethodCanceledError: MethodCanceledError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has failed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param childMethodFailedError Error describing the failed method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodFailed(
  @SerialName("childFailedWorkflowError")
  val childMethodFailedError: MethodFailedError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has timed out.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param childMethodTimedOutError Error describing the timed out method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodTimedOut(
  val childMethodTimedOutError: WorkflowMethodTimedOutError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent

/**
 * This message is an event telling a workflow that another workflow's method has completed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param childWorkflowReturnValue Return value of the completed method
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class ChildMethodCompleted(
  val childWorkflowReturnValue: WorkflowReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent

/**
 * This message is an event telling a workflow that a task was canceled.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param taskCanceledError Error describing the canceled task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskCanceled(
  @SerialName("canceledTaskError")
  val taskCanceledError: TaskCanceledError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskCanceledError.taskId

  override fun serviceName() = taskCanceledError.serviceName
}

/**
 * This message is an event telling a workflow that a task has failed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param taskFailedError Error describing the failed task
 * @param deferredError if the task is a workflow task, and the failure is due to a deferred error
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskFailed(
  @SerialName("failedTaskError")
  val taskFailedError: TaskFailedError,
  val deferredError: DeferredError?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskFailedError.taskId

  override fun serviceName() = taskFailedError.serviceName
}

/**
 * This message is an event telling a workflow that a task has (global) timed out.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param taskTimedOutError Error describing the timed out task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskTimedOut(
  val taskTimedOutError: TaskTimedOutError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskTimedOutError.taskId

  override fun serviceName() = taskTimedOutError.serviceName
}

/**
 * This message is an event telling a workflow that a task has completed.
 *
 * @param workflowName Name of the workflow receiving the event
 * @param workflowId Id of the workflow receiving the event
 * @param workflowMethodId Id of the method receiving the event
 * @param taskReturnValue Return value of the completed task
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TaskCompleted(
  val taskReturnValue: TaskReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), TaskEvent {
  override fun taskId() = taskReturnValue.taskId

  override fun serviceName() = taskReturnValue.serviceName
}

/**
 * This message is an event telling a running workflow that a timer has completed
 *
 * @param workflowName Name of the workflow to complete the timer for
 * @param workflowId Id of the workflow to complete the timer for
 * @param workflowMethodId Id of the method to complete the timer for
 * @param timerId Id of the timer to complete
 * @param emitterName Name of the emitter
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class TimerCompleted(
  val timerId: TimerId,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowEngineMessage(), WorkflowMethodEvent
