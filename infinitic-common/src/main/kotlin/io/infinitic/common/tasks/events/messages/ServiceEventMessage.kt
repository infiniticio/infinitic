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
package io.infinitic.common.tasks.events.messages

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.Version
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.currentVersion
import io.infinitic.exceptions.DeferredException
import kotlinx.serialization.Serializable
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed as TaskFailedWorkflow

@Serializable
sealed class ServiceEventMessage : Message {
  val version: Version = Version(currentVersion)
  abstract val taskId: TaskId
  abstract val serviceName: ServiceName
  abstract val methodName: MethodName
  abstract val taskRetrySequence: TaskRetrySequence
  abstract val taskRetryIndex: TaskRetryIndex
  abstract val requester: Requester
  abstract val clientWaiting: Boolean?
  abstract val taskTags: Set<TaskTag>
  abstract val taskMeta: TaskMeta

  override fun key() = null

  override fun entity() = when (isWorkflowTask()) {
    true -> requester.workflowName!!.toString()
    false -> serviceName.toString()
  }

  fun isWorkflowTask() = (serviceName == ServiceName(WorkflowTask::class.java.name))

}

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
data class TaskStartedEvent(
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val methodName: MethodName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
  override val taskRetrySequence: TaskRetrySequence,
  override val taskRetryIndex: TaskRetryIndex,
  override val requester: Requester,
  override val clientWaiting: Boolean?,
  override val taskTags: Set<TaskTag>,
  override val taskMeta: TaskMeta,
  val workflowVersion: WorkflowVersion?
) : ServiceEventMessage() {
  companion object {
    fun from(msg: ExecuteTask, emitterName: EmitterName) = TaskStartedEvent(
        serviceName = msg.serviceName,
        methodName = msg.methodName,
        taskId = msg.taskId,
        emitterName = emitterName,
        taskRetrySequence = msg.taskRetrySequence,
        taskRetryIndex = msg.taskRetryIndex,
        requester = msg.requester ?: thisShouldNotHappen(),
        clientWaiting = msg.clientWaiting,
        taskTags = msg.taskTags,
        taskMeta = msg.taskMeta,
        workflowVersion = msg.workflowVersion,
    )
  }
}

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
data class TaskFailedEvent(
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val methodName: MethodName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
  override val taskRetrySequence: TaskRetrySequence,
  override val taskRetryIndex: TaskRetryIndex,
  override val requester: Requester,
  override val clientWaiting: Boolean?,
  override val taskTags: Set<TaskTag>,
  override val taskMeta: TaskMeta,
  val executionError: ExecutionError,
  val deferredError: DeferredError?,
  val workflowVersion: WorkflowVersion?,
) : ServiceEventMessage() {

  fun getEventForClient(emitterName: EmitterName) = when (requester) {
    is WorkflowRequester -> null
    is ClientRequester -> TaskFailedClient(
        recipientName = requester.clientName,
        taskId = taskId,
        cause = executionError,
        emitterName = emitterName,
    )
  }

  fun getEventForWorkflow(emitterName: EmitterName, emittedAt: MillisInstant) = when (requester) {
    is WorkflowRequester -> TaskFailedWorkflow(
        workflowId = requester.workflowId,
        workflowName = requester.workflowName,
        workflowMethodName = requester.workflowMethodName,
        workflowMethodId = requester.workflowMethodId,
        taskFailedError = TaskFailedError(
            serviceName = serviceName,
            methodName = methodName,
            taskId = taskId,
            cause = executionError,
        ),
        deferredError = deferredError,
        emitterName = emitterName,
        emittedAt = emittedAt,
    )

    is ClientRequester -> null
  }

  companion object {
    fun from(
      msg: ExecuteTask,
      emitterName: EmitterName,
      cause: Throwable,
      meta: MutableMap<String, ByteArray>
    ) = TaskFailedEvent(
        serviceName = msg.serviceName,
        methodName = msg.methodName,
        taskId = msg.taskId,
        emitterName = emitterName,
        taskRetrySequence = msg.taskRetrySequence,
        taskRetryIndex = msg.taskRetryIndex,
        requester = msg.requester ?: thisShouldNotHappen(),
        clientWaiting = msg.clientWaiting,
        taskTags = msg.taskTags,
        taskMeta = TaskMeta(meta),
        executionError = cause.getExecutionError(emitterName),
        deferredError = cause.deferredError,
        workflowVersion = msg.workflowVersion,
    )
  }
}

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
data class TaskRetriedEvent(
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val methodName: MethodName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
  override val taskRetrySequence: TaskRetrySequence,
  override val taskRetryIndex: TaskRetryIndex,
  override val requester: Requester,
  override val clientWaiting: Boolean?,
  override val taskTags: Set<TaskTag>,
  override val taskMeta: TaskMeta,

  val taskRetryDelay: MillisDuration,
  val lastError: ExecutionError,
) : ServiceEventMessage() {

  companion object {
    fun from(
      msg: ExecuteTask,
      emitterName: EmitterName,
      cause: Throwable,
      delay: MillisDuration,
      meta: MutableMap<String, ByteArray>
    ) = TaskRetriedEvent(
        serviceName = msg.serviceName,
        methodName = msg.methodName,
        taskId = msg.taskId,
        emitterName = emitterName,
        taskRetrySequence = msg.taskRetrySequence,
        taskRetryIndex = msg.taskRetryIndex + 1,
        requester = msg.requester ?: thisShouldNotHappen(),
        clientWaiting = msg.clientWaiting,
        taskTags = msg.taskTags,
        taskMeta = TaskMeta(meta),
        taskRetryDelay = delay,
        lastError = cause.getExecutionError(emitterName),
    )
  }
}

@Serializable
@AvroNamespace("io.infinitic.tasks.events")
data class TaskCompletedEvent(
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val methodName: MethodName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
  override val taskRetrySequence: TaskRetrySequence,
  override val taskRetryIndex: TaskRetryIndex,
  override val requester: Requester,
  override val clientWaiting: Boolean?,
  override val taskTags: Set<TaskTag>,
  override val taskMeta: TaskMeta,
  val returnValue: ReturnValue,
  val workflowVersion: WorkflowVersion?,
) : ServiceEventMessage() {

  fun getEventForClient(emitterName: EmitterName) = when (requester) {
    is ClientRequester -> TaskCompletedClient(
        recipientName = requester.clientName,
        taskId = taskId,
        taskReturnValue = returnValue,
        taskMeta = taskMeta,
        emitterName = emitterName,
    )

    is WorkflowRequester -> null
  }

  fun getEventForWorkflow(emitterName: EmitterName, emittedAt: MillisInstant) = when (requester) {
    is ClientRequester -> null
    is WorkflowRequester -> TaskCompletedWorkflow(
        workflowId = requester.workflowId,
        workflowName = requester.workflowName,
        workflowMethodName = requester.workflowMethodName,
        workflowMethodId = requester.workflowMethodId,
        taskReturnValue = TaskReturnValue(
            serviceName = serviceName,
            taskId = taskId,
            methodName = methodName,
            taskMeta = taskMeta,
            returnValue = returnValue,
        ),
        emitterName = emitterName,
        emittedAt = emittedAt,
    )
  }

  fun getEventsForTag(emitterName: EmitterName) = taskTags.map {
    RemoveTagFromTask(
        taskTag = it,
        serviceName = serviceName,
        taskId = taskId,
        emitterName = emitterName,
    )
  }

  companion object {
    fun from(
      msg: ExecuteTask,
      emitterName: EmitterName,
      value: Any?,
      meta: MutableMap<String, ByteArray>
    ) = TaskCompletedEvent(
        serviceName = msg.serviceName,
        methodName = msg.methodName,
        taskId = msg.taskId,
        emitterName = emitterName,
        taskRetrySequence = msg.taskRetrySequence,
        taskRetryIndex = msg.taskRetryIndex,
        requester = msg.requester ?: thisShouldNotHappen(),
        clientWaiting = msg.clientWaiting,
        taskTags = msg.taskTags,
        taskMeta = TaskMeta(meta),
        returnValue = ReturnValue.from(value),
        workflowVersion = msg.workflowVersion,
    )
  }
}

val ExecuteTask.clientName
  get() = if (workflowName == null) ClientName.from(emitterName) else null

val Throwable.deferredError
  get() = when (this is DeferredException) {
    true -> DeferredError.from(this)
    false -> null
  }

fun Throwable.getExecutionError(emitterName: EmitterName) =
    ExecutionError.from(WorkerName.from(emitterName), this)
