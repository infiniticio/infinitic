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

package io.infinitic.common.clients.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.clients.messages.interfaces.WorkflowMessage
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.errors.Error
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage : Message {
    val messageId: MessageId = MessageId()
    abstract val clientName: ClientName

    override fun envelope() = ClientEnvelope.from(this)
}

@Serializable
data class TaskCompleted(
    override val clientName: ClientName,
    override val taskId: TaskId,
    val taskReturnValue: MethodReturnValue,
    val taskMeta: TaskMeta
) : ClientMessage(), TaskMessage

@Serializable
data class TaskFailed(
    override val clientName: ClientName,
    override val taskId: TaskId,
    val error: Error,
) : ClientMessage(), TaskMessage

@Serializable
data class TaskCanceled(
    override val clientName: ClientName,
    override val taskId: TaskId,
    val taskMeta: TaskMeta
) : ClientMessage(), TaskMessage

@Serializable
data class UnknownTask(
    override val clientName: ClientName,
    override val taskId: TaskId
) : ClientMessage(), TaskMessage

@Serializable
data class TaskIdsPerTag(
    override val clientName: ClientName,
    val taskName: TaskName,
    val taskTag: TaskTag,
    val taskIds: Set<TaskId>
) : ClientMessage()

@Serializable
data class WorkflowCompleted(
    override val clientName: ClientName,
    override val workflowId: WorkflowId,
    val workflowReturnValue: MethodReturnValue
) : ClientMessage(), WorkflowMessage

@Serializable
data class WorkflowFailed(
    override val clientName: ClientName,
    override val workflowId: WorkflowId,
    val error: Error
) : ClientMessage(), WorkflowMessage

@Serializable
data class WorkflowCanceled(
    override val clientName: ClientName,
    override val workflowId: WorkflowId
) : ClientMessage(), WorkflowMessage

@Serializable
data class UnknownWorkflow(
    override val clientName: ClientName,
    override val workflowId: WorkflowId
) : ClientMessage(), WorkflowMessage

@Serializable
data class WorkflowAlreadyCompleted(
    override val clientName: ClientName,
    override val workflowId: WorkflowId
) : ClientMessage(), WorkflowMessage

@Serializable
data class WorkflowIdsPerTag(
    override val clientName: ClientName,
    val workflowName: WorkflowName,
    val workflowTag: WorkflowTag,
    val workflowIds: Set<WorkflowId>
) : ClientMessage()
