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

package io.infinitic.common.workflows.tags.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.messages.Message
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowTagEngineMessage : Message {
    val messageId = MessageId()
    abstract val emitterName: ClientName
    abstract val workflowTag: WorkflowTag
    abstract val workflowName: WorkflowName

    override fun envelope() = WorkflowTagEngineEnvelope.from(this)
}

@Serializable
data class SendSignalByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val channelName: ChannelName,
    val channelSignalId: ChannelSignalId,
    val channelSignal: ChannelSignal,
    val channelSignalTypes: Set<ChannelSignalType>,
    var emitterWorkflowId: WorkflowId?,
    override val emitterName: ClientName
) : WorkflowTagEngineMessage()

@Serializable
data class CancelWorkflowByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val reason: WorkflowCancellationReason,
    var emitterWorkflowId: WorkflowId?,
    override val emitterName: ClientName
) : WorkflowTagEngineMessage()

@Serializable
data class RetryWorkflowTaskByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    var emitterWorkflowId: WorkflowId?,
    override val emitterName: ClientName,
) : WorkflowTagEngineMessage()

@Serializable
data class AddTagToWorkflow(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val workflowId: WorkflowId,
    override val emitterName: ClientName,
) : WorkflowTagEngineMessage()

@Serializable
data class RemoveTagFromWorkflow(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val workflowId: WorkflowId,
    override val emitterName: ClientName,
) : WorkflowTagEngineMessage()

@Serializable
data class GetWorkflowIdsByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    override val emitterName: ClientName,
) : WorkflowTagEngineMessage()

@Serializable
data class DispatchMethodByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    var parentWorkflowId: WorkflowId?,
    var parentWorkflowName: WorkflowName?,
    var parentMethodRunId: MethodRunId?,
    val methodRunId: MethodRunId,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val clientWaiting: Boolean,
    override val emitterName: ClientName,
) : WorkflowTagEngineMessage()
