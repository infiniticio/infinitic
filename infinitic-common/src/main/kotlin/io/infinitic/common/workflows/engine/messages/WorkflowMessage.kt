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
import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.clients.messages.MethodTimedOut
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.Version
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.waitingClients
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowMethodId
import io.infinitic.common.requester.workflowMethodName
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.DelegatedTaskData
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
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.currentVersion
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface WorkflowMessageInterface : Message {
  override val messageId: MessageId
  val version: Version?
  val workflowId: WorkflowId
  val workflowName: WorkflowName
  fun isWorkflowTaskEvent() =
      (this is TaskEvent) && this.serviceName() == WorkflowTask.SERVICE_NAME
}

interface WorkflowEvent

interface MethodEvent : WorkflowEvent {
  val workflowVersion: WorkflowVersion?
  val workflowMethodName: MethodName?
  val workflowMethodId: WorkflowMethodId
}

interface TaskEvent : MethodEvent {
  fun taskId(): TaskId
  fun serviceName(): ServiceName
}

@Serializable
sealed class WorkflowMessage : WorkflowMessageInterface {
  @AvroDefault(Avro.NULL)
  override val version: Version? = Version(currentVersion)
  override val messageId: MessageId = MessageId()
  override fun key() = workflowId.toString()
  override fun entity() = workflowName.toString()
}

/**
 * This command tells the workflow to retry its workflow task.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RetryWorkflowTask(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage

/**
 * This command tells the workflow to retry some tasks.
 *
 * The tasks to retry are selected by their id, status or service name.
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
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage

/**
 * This message tells the workflow's method that a new client is waiting for its output
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WaitWorkflow(
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage

/**
 * This command dispatches a new workflow.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("DispatchWorkflow")
data class DispatchWorkflow(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  @AvroDefault(Avro.NULL) val workflowTaskId: TaskId? = null,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowName: WorkflowName? = null,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowId: WorkflowId? = null,
  @Deprecated("Not used since version 0.13.0") val parentMethodRunId: WorkflowMethodId? = null,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
) : WorkflowMessage(), WorkflowCmdMessage {

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

  fun workflowMethod() = WorkflowMethod(
      workflowMethodId = WorkflowMethodId.from(workflowId),
      waitingClients = requester?.waitingClients(clientWaiting) ?: waitingClients(),
      requester = requester,
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

  fun methodDispatchedEvent(emitterName: EmitterName) = MethodCommandedEvent(
      workflowName = workflowName,
      workflowVersion = null,
      workflowId = workflowId,
      workflowMethodId = WorkflowMethodId.from(workflowId),
      methodName = methodName,
      methodParameters = methodParameters,
      methodParameterTypes = methodParameterTypes,
      requester = requester ?: thisShouldNotHappen(),
      emitterName = emitterName,
  )

  fun childMethodDispatchedEvent(emitterName: EmitterName) = RemoteMethodDispatchedEvent(
      remoteMethodDispatched = RemoteMethodDispatchedDesc(
          workflowId = workflowId,
          workflowName = workflowName,
          workflowMethodName = methodName,
          workflowMethodId = WorkflowMethodId.from(workflowId),
          methodName = methodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
      ),
      workflowName = requester.workflowName ?: thisShouldNotHappen(),
      workflowId = requester.workflowId ?: thisShouldNotHappen(),
      workflowVersion = requester.workflowVersion,
      workflowMethodName = requester.workflowMethodName ?: thisShouldNotHappen(),
      workflowMethodId = requester.workflowMethodId ?: thisShouldNotHappen(),
      emitterName = emitterName,
  )

  fun childMethodTimedOut(emitterName: EmitterName, timeoutDuration: MillisDuration) =
      RemoteMethodTimedOut(
          childMethodTimedOutError = MethodTimedOutError(
              workflowName = workflowName,
              workflowId = workflowId,
              workflowMethodName = methodName,
              workflowMethodId = WorkflowMethodId.from(workflowId),
          ),
          workflowName = requester.workflowName ?: thisShouldNotHappen(),
          workflowId = requester.workflowId ?: thisShouldNotHappen(),
          workflowVersion = requester.workflowVersion,
          workflowMethodName = requester.workflowMethodName,
          workflowMethodId = requester.workflowMethodId ?: thisShouldNotHappen(),
          emitterName = emitterName,
          emittedAt = (emittedAt ?: thisShouldNotHappen()) + timeoutDuration,
      )

  fun waitingClients() = when (clientWaiting) {
    true -> mutableSetOf((requester as ClientRequester).clientName)
    false -> mutableSetOf()
  }
}


/**
 * This command tells the workflow to dispatch a new method.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("DispatchMethod")
data class DispatchMethod(
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId,
  @AvroName("methodName") val workflowMethodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowId: WorkflowId? = null,
  @Deprecated("Not used since version 0.13.0") val parentWorkflowName: WorkflowName? = null,
  @Deprecated("Not used since version 0.13.0") val parentMethodRunId: WorkflowMethodId? = null,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  val clientWaiting: Boolean,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowCmdMessage, WorkflowEvent {
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

  fun childMethodDispatchedEvent(emitterName: EmitterName) = RemoteMethodDispatchedEvent(
      remoteMethodDispatched = RemoteMethodDispatchedDesc(
          workflowId = workflowId,
          workflowName = workflowName,
          workflowMethodName = workflowMethodName,
          workflowMethodId = workflowMethodId,
          methodName = workflowMethodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
      ),
      workflowName = requester.workflowName ?: thisShouldNotHappen(),
      workflowId = requester.workflowId ?: thisShouldNotHappen(),
      workflowVersion = requester.workflowVersion,
      workflowMethodName = requester.workflowMethodName ?: thisShouldNotHappen(),
      workflowMethodId = requester.workflowMethodId ?: thisShouldNotHappen(),
      emitterName = emitterName,
  )

  fun childMethodTimedOut(emitterName: EmitterName, timeoutDuration: MillisDuration) =
      RemoteMethodTimedOut(
          childMethodTimedOutError = MethodTimedOutError(
              workflowName = workflowName,
              workflowId = workflowId,
              workflowMethodName = workflowMethodName,
              workflowMethodId = workflowMethodId,
          ),
          workflowName = requester.workflowName ?: thisShouldNotHappen(),
          workflowId = requester.workflowId ?: thisShouldNotHappen(),
          workflowVersion = requester.workflowVersion,
          workflowMethodName = requester.workflowMethodName,
          workflowMethodId = requester.workflowMethodId ?: thisShouldNotHappen(),
          emitterName = emitterName,
          emittedAt = (emittedAt ?: thisShouldNotHappen()) + timeoutDuration,
      )
}


/**
 * This command tells the workflow to complete running timers.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CompleteTimers(
  @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage, WorkflowEvent

/**
 * This command tells the workflow to cancel itself
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
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage, WorkflowEvent

/**
 * This command tells the workflow to complete itself
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class CompleteWorkflow(
  val workflowReturnValue: ReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage, WorkflowEvent

/**
 * This command sends a signal to the workflow
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
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?,
  @AvroDefault(Avro.NULL) override val requester: Requester?,
) : WorkflowMessage(), WorkflowCmdMessage, WorkflowEvent

/**
 * This event tells the workflow that the method of another workflow is unknown.
 *
 * This event is received when a workflow waits for the completion of
 * another workflow method that is already completed or does not exist.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("ChildMethodUnknown")
data class RemoteMethodUnknown(
  @SerialName("childUnknownWorkflowError")
  val childMethodUnknownError: MethodUnknownError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells the workflow that the method of another workflow has been canceled.
 *
 * This event is received if the workflow dispatched the method itself
 * or asked for its status / result
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("ChildMethodCanceled")
data class RemoteMethodCanceled(
  @SerialName("childCanceledWorkflowError")
  val childMethodCanceledError: MethodCanceledError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells the workflow that the method of another workflow has failed.
 *
 * This event is received if the workflow dispatched the method itself
 * or asked for its status / result
 * */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("ChildMethodFailed")
data class RemoteMethodFailed(
  @SerialName("childFailedWorkflowError")
  val childMethodFailedError: MethodFailedError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells the workflow that the method of another workflow has timed out.
 *
 * This event is received if the workflow dispatched the method itself
 * or asked for its status / result
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("ChildMethodTimedOut")
data class RemoteMethodTimedOut(
  val childMethodTimedOutError: MethodTimedOutError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells the workflow that the method of another workflow has completed.
 *
 * This event is received if the workflow dispatched the method itself
 * or asked for its status / result
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("ChildMethodCompleted")
data class RemoteMethodCompleted(
  val childWorkflowReturnValue: WorkflowReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells the workflow that a task was canceled.
 *
 * (This cancellation is done at workflow level, it will still be executed)
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("TaskCanceled")
data class RemoteTaskCanceled(
  @SerialName("canceledTaskError")
  val taskCanceledError: TaskCanceledError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, TaskEvent {
  override fun taskId() = taskCanceledError.taskId

  override fun serviceName() = taskCanceledError.serviceName
}

/**
 * This event tells the workflow that a task has failed.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("TaskFailed")
data class RemoteTaskFailed(
  @SerialName("failedTaskError")
  val taskFailedError: TaskFailedError,
  val deferredError: DeferredError?,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, TaskEvent {
  override fun taskId() = taskFailedError.taskId

  override fun serviceName() = taskFailedError.serviceName
}

/**
 * This event tells the workflow that a task has timed out.
 *
 * This global timeout is at workflow level
 * An execution timeout, will be seen as a failed task
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("TaskTimedOut")
data class RemoteTaskTimedOut(
  val taskTimedOutError: TaskTimedOutError,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, TaskEvent {
  override fun taskId() = taskTimedOutError.taskId

  override fun serviceName() = taskTimedOutError.serviceName
}


/**
 * This event tells the workflow that a task has completed.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("TaskCompleted")
data class RemoteTaskCompleted(
  val taskReturnValue: TaskReturnValue,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, TaskEvent {
  override fun taskId() = taskReturnValue.taskId

  override fun serviceName() = taskReturnValue.serviceName

  companion object {
    fun from(
      data: DelegatedTaskData,
      returnValue: ReturnValue,
      emitterName: EmitterName,
      emittedAt: MillisInstant
    ) = when (data.requester) {
      is WorkflowRequester -> RemoteTaskCompleted(
          taskReturnValue = data.taskReturnData(returnValue),
          workflowName = data.requester.workflowName,
          workflowId = data.requester.workflowId,
          workflowVersion = data.requester.workflowVersion,
          workflowMethodName = data.requester.workflowMethodName,
          workflowMethodId = data.requester.workflowMethodId,
          emitterName = emitterName,
          emittedAt = emittedAt,
      )

      is ClientRequester -> null
    }
  }
}

/**
 * This event tells the workflow that a timer has completed.
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
@AvroName("TimerCompleted")
data class RemoteTimerCompleted(
  val timerId: TimerId,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  @AvroDefault(Avro.NULL) override val workflowVersion: WorkflowVersion?,
  @AvroDefault(Avro.NULL) override val workflowMethodName: MethodName?,
  @AvroName("methodRunId") override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
  @AvroDefault(Avro.NULL) override var emittedAt: MillisInstant?
) : WorkflowMessage(), WorkflowEngineMessage, MethodEvent

/**
 * This event tells us that the workflow has completed
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowCompletedEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage

/**
 * This event tells us that the workflow was canceled
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowCanceledEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage

/**
 * This event tells us that a new method has been commanded on this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MethodCommandedEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  val workflowMethodId: WorkflowMethodId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?,
  val requester: Requester,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage

/**
 * This event tells us that a method has completed on this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MethodCompletedEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val awaitingRequesters: Set<Requester>,
  override val emitterName: EmitterName,
  val returnValue: ReturnValue,
) : WorkflowMessage(), WorkflowEventMessage, MethodTerminated {
  override fun getEventForAwaitingClients(emitterName: EmitterName) =
      awaitingRequesters.filterIsInstance<ClientRequester>().map { requester ->
        MethodCompleted(
            recipientName = requester.clientName,
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            methodReturnValue = returnValue,
            emitterName = emitterName,
        )
      }

  override fun getEventForAwaitingWorkflows(emitterName: EmitterName, emittedAt: MillisInstant) =
      awaitingRequesters.filterIsInstance<WorkflowRequester>().map { requester ->
        RemoteMethodCompleted(
            childWorkflowReturnValue = WorkflowReturnValue(
                workflowId = workflowId,
                workflowMethodId = workflowMethodId,
                returnValue = returnValue,
            ),
            workflowName = requester.workflowName,
            workflowId = requester.workflowId,
            workflowVersion = requester.workflowVersion,
            workflowMethodName = requester.workflowMethodName,
            workflowMethodId = requester.workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )
      }
}

/**
 * This event tells us that a method has failed on this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MethodFailedEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val awaitingRequesters: Set<Requester>,
  override val emitterName: EmitterName,
  val deferredError: DeferredError
) : WorkflowMessage(), WorkflowEventMessage, MethodTerminated {
  override fun getEventForAwaitingClients(emitterName: EmitterName) =
      awaitingRequesters.filterIsInstance<ClientRequester>().map { requester ->
        MethodFailed(
            recipientName = requester.clientName,
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            cause = deferredError,
            emitterName = emitterName,
        )
      }

  override fun getEventForAwaitingWorkflows(emitterName: EmitterName, emittedAt: MillisInstant) =
      awaitingRequesters.filterIsInstance<WorkflowRequester>().map { requester ->
        RemoteMethodFailed(
            childMethodFailedError = MethodFailedError(
                workflowName = workflowName,
                workflowId = workflowId,
                workflowMethodId = workflowMethodId,
                workflowMethodName = workflowMethodName,
                deferredError = deferredError,
            ),
            workflowName = requester.workflowName,
            workflowId = requester.workflowId,
            workflowVersion = requester.workflowVersion,
            workflowMethodName = requester.workflowMethodName,
            workflowMethodId = requester.workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )
      }
}

/**
 * This event tells us that a method was canceled on this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MethodCanceledEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val awaitingRequesters: Set<Requester>,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage, MethodTerminated {

  override fun getEventForAwaitingClients(emitterName: EmitterName) =
      awaitingRequesters.filterIsInstance<ClientRequester>().map { requester ->
        MethodCanceled(
            recipientName = requester.clientName,
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            emitterName = emitterName,
        )
      }

  override fun getEventForAwaitingWorkflows(emitterName: EmitterName, emittedAt: MillisInstant) =
      awaitingRequesters.filterIsInstance<WorkflowRequester>().map { requester ->
        RemoteMethodCanceled(
            childMethodCanceledError = MethodCanceledError(
                workflowName = workflowName,
                workflowId = workflowId,
                workflowMethodName = workflowMethodName,
                workflowMethodId = workflowMethodId,
            ),
            workflowId = requester.workflowId,
            workflowName = requester.workflowName,
            workflowVersion = requester.workflowVersion,
            workflowMethodName = requester.workflowMethodName,
            workflowMethodId = requester.workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )
      }
}

/**
 * This event tells us that a method has timed out on this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class MethodTimedOutEvent(
  override val workflowName: WorkflowName,
  override val workflowVersion: WorkflowVersion?,
  override val workflowId: WorkflowId,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val awaitingRequesters: Set<Requester>,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage, MethodTerminated {
  override fun getEventForAwaitingClients(emitterName: EmitterName) =
      awaitingRequesters.filterIsInstance<ClientRequester>().map { requester ->
        MethodTimedOut(
            recipientName = requester.clientName,
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            emitterName = emitterName,
        )
      }

  override fun getEventForAwaitingWorkflows(emitterName: EmitterName, emittedAt: MillisInstant) =
      awaitingRequesters.filterIsInstance<WorkflowRequester>().map { requester ->
        RemoteMethodTimedOut(
            childMethodTimedOutError = MethodTimedOutError(
                workflowName = workflowName,
                workflowId = workflowId,
                workflowMethodId = workflowMethodId,
                workflowMethodName = workflowMethodName,
            ),
            workflowId = requester.workflowId,
            workflowName = requester.workflowName,
            workflowVersion = requester.workflowVersion,
            workflowMethodName = requester.workflowMethodName,
            workflowMethodId = requester.workflowMethodId,
            emitterName = emitterName,
            emittedAt = emittedAt,
        )
      }
}

/**
 * This event tells us that a task was dispatched by this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RemoteTaskDispatchedEvent(
  val remoteTaskDispatched: RemoteTaskDispatchedDesc,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowVersion: WorkflowVersion?,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage, MethodEvent, TaskEvent {
  override fun taskId() = remoteTaskDispatched.taskId
  override fun serviceName() = remoteTaskDispatched.serviceName
}

/**
 * This event tells us that a child method was dispatched by this workflow
 */
@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RemoteMethodDispatchedEvent(
  val remoteMethodDispatched: RemoteMethodDispatchedDesc,
  override val workflowName: WorkflowName,
  override val workflowId: WorkflowId,
  override val workflowVersion: WorkflowVersion?,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val emitterName: EmitterName,
) : WorkflowMessage(), WorkflowEventMessage, MethodEvent


@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RemoteMethodDispatchedDesc(
  val workflowId: WorkflowId,
  val workflowName: WorkflowName,
  val workflowMethodName: MethodName,
  val workflowMethodId: WorkflowMethodId,
  val methodName: MethodName,
  val methodParameters: MethodParameters,
  val methodParameterTypes: MethodParameterTypes?
)

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class RemoteTaskDispatchedDesc(
  val taskId: TaskId,
  val taskName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodParameters,
  val serviceName: ServiceName,
)
