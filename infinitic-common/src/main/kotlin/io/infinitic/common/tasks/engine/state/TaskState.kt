/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.tasks.engine.state

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.errors.Error
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
data class TaskState(
    val waitingClients: Set<ClientName>,
    val lastMessageId: MessageId,
    val taskId: TaskId,
    val taskName: TaskName,
    val taskReturnValue: MethodReturnValue?,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val workflowId: WorkflowId?,
    val workflowName: WorkflowName?,
    val methodRunId: MethodRunId?,
    val taskStatus: TaskStatus,
    var taskRetrySequence: TaskRetrySequence = TaskRetrySequence(0),
    var taskAttemptId: TaskAttemptId,
    var taskRetryIndex: TaskRetryIndex = TaskRetryIndex(0),
    var lastError: Error? = null,
    val taskTags: Set<TaskTag>,
    val taskOptions: TaskOptions,
    val taskMeta: TaskMeta
) {
    companion object {
        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())

    fun deepCopy() = fromByteArray(toByteArray())
}
