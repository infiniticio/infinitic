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

package io.infinitic.logs.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

interface Log {
  val timestamp: Long

  fun toByteArray(): ByteArray
}

@Serializable
sealed interface TaskLog : Log {
  val serviceName: ServiceName
  val taskId: TaskId
}

@Serializable
sealed interface TaskCommandLog : TaskLog {
  val requester: Requester
}

@Serializable
sealed interface TaskEventLog : TaskLog {
  val workerName: String
}

@Serializable
data class RunTask(
  override val timestamp: Long,
  override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val requester: Requester,
  val methodCall: MethodCall,
  val context: TaskContext,
  val meta: TaskMeta,
  val tags: Set<TaskTag>,
) : TaskCommandLog {
  override fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, RunTask::class)
  }
}


@Serializable
data class TaskFailed(
  override val timestamp: Long,
  override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val workerName: String,
  val requester: Requester,
  val error: TaskError,
) : TaskEventLog {
  override fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, TaskFailed::class)
  }
}

@Serializable
data class TaskCompleted(
  override val timestamp: Long,
  override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val workerName: String,
  val requester: Requester,
  val result: ReturnValue
) : TaskEventLog {
  override fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, TaskCompleted::class)
  }
}

@Serializable
data class MethodCall(
  val methodName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodParameters
)

@Serializable
data class TaskContext(
  val retrySequence: TaskRetrySequence,
  val retryIndex: TaskRetryIndex,
  val lastError: TaskError?,
)

@Serializable
sealed interface Requester

@Serializable
data class ClientRequester(
  val clientName: ClientName,
  val clientWaiting: Boolean
) : Requester

@Serializable
data class WorkflowRequester(
  val workflowId: WorkflowId,
  val workflowName: WorkflowName,
  val workflowMethodId: MethodRunId,
) : Requester

@Serializable
data class TaskError(
  val errorName: String,
  val errorMessage: String,
  val errorStackTrace: String
)
