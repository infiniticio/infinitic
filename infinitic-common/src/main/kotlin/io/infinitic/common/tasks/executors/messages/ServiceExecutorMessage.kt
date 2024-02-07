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
package io.infinitic.common.tasks.executors.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.Version
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowMethodId
import io.infinitic.common.requester.workflowMethodName
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.RemoteTaskDispatchedDesc
import io.infinitic.common.workflows.engine.messages.RemoteTaskDispatchedEvent
import io.infinitic.currentVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServiceExecutorMessage : Message {
  @Suppress("RedundantNullableReturnType")
  @AvroDefault(Avro.NULL)
  val version: Version? = Version(currentVersion)
  abstract val serviceName: ServiceName
  abstract val taskId: TaskId
  abstract val taskRetrySequence: TaskRetrySequence
  abstract val taskRetryIndex: TaskRetryIndex
  abstract val requester: Requester?

  override fun key() = null

  override fun entity() = when (isWorkflowTask()) {
    true -> requester.workflowName!!.toString()
    false -> serviceName.toString()
  }

  fun isWorkflowTask() = (serviceName == WorkflowTask.SERVICE_NAME)
}

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
data class ExecuteTask(
  @AvroDefault(Avro.NULL) override val messageId: MessageId? = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
  override val taskRetrySequence: TaskRetrySequence,
  override val taskRetryIndex: TaskRetryIndex,
  @AvroDefault(Avro.NULL) override var requester: Requester?,
  @Deprecated("Not used since version 0.13.0") val workflowName: WorkflowName? = null,
  @Deprecated("Not used since version 0.13.0") val workflowId: WorkflowId? = null,
  @Deprecated("Not used since version 0.13.0") @AvroName("methodRunId") val workflowMethodId: WorkflowMethodId? = null,
  val taskTags: Set<TaskTag>,
  val taskMeta: TaskMeta,
  val clientWaiting: Boolean,
  val methodName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodParameters,
  val lastError: ExecutionError?,
  @Deprecated("Not used since version 0.13.0") @AvroDefault(Avro.NULL) val workflowVersion: WorkflowVersion? = null
) : ServiceExecutorMessage() {

  init {
    // this is used only to handle previous messages that are still on <0.13 version
    // in topics or in bufferedMessages of a workflow state
    requester = requester ?: when (workflowId) {
      null -> ClientRequester(clientName = ClientName.from(emitterName))
      else -> WorkflowRequester(
          workflowId = workflowId,
          workflowName = workflowName ?: WorkflowName("undefined"),
          workflowVersion = null,
          workflowMethodName = MethodName("undefined"),
          workflowMethodId = workflowMethodId ?: WorkflowMethodId("undefined"),
      )
    }
  }

  companion object {
    fun retryFrom(
      msg: ExecuteTask,
      emitterName: EmitterName,
      cause: Throwable,
      meta: MutableMap<String, ByteArray>
    ) = ExecuteTask(
        serviceName = msg.serviceName,
        taskId = msg.taskId,
        emitterName = emitterName,
        taskRetrySequence = msg.taskRetrySequence,
        taskRetryIndex = msg.taskRetryIndex + 1,
        requester = msg.requester,
        taskTags = msg.taskTags,
        taskMeta = TaskMeta(meta),
        clientWaiting = msg.clientWaiting,
        methodName = msg.methodName,
        methodParameterTypes = msg.methodParameterTypes,
        methodParameters = msg.methodParameters,
        lastError = cause.getExecutionError(emitterName),
    )
  }

  fun taskDispatchedEvent(emitterName: EmitterName) = RemoteTaskDispatchedEvent(
      remoteTaskDispatched = RemoteTaskDispatchedDesc(
          taskId = taskId,
          taskName = methodName,
          methodParameterTypes = methodParameterTypes,
          methodParameters = methodParameters,
          serviceName = serviceName,
      ),
      workflowName = requester.workflowName!!,
      workflowId = requester.workflowId!!,
      workflowVersion = requester.workflowVersion,
      workflowMethodName = requester.workflowMethodName!!,
      workflowMethodId = requester.workflowMethodId!!,
      emitterName = emitterName,
  )
}

private fun Throwable.getExecutionError(emitterName: EmitterName) =
    ExecutionError.from(WorkerName.from(emitterName), this)
