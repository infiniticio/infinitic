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
import io.infinitic.common.messages.Message
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkflowTagEngineMessage : Message {
    val messageId = MessageId()
    abstract val workflowTag: WorkflowTag
    abstract val workflowName: WorkflowName

    override fun envelope() = WorkflowTagEngineEnvelope.from(this)
}

@Serializable
data class SendToChannelPerTag(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName,
    val clientName: ClientName,
    val clientWaiting: Boolean,
    val channelEventId: ChannelEventId,
    val channelName: ChannelName,
    val channelEvent: ChannelEvent,
    val channelEventTypes: Set<ChannelEventType>
) : WorkflowTagEngineMessage()

@Serializable
data class CancelWorkflowPerTag(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName,
    val reason: WorkflowCancellationReason
) : WorkflowTagEngineMessage()

@Serializable
data class RetryWorkflowTaskPerTag(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName
) : WorkflowTagEngineMessage()

@Serializable
data class AddWorkflowTag(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName,
    val workflowId: WorkflowId,
) : WorkflowTagEngineMessage()

@Serializable
data class RemoveWorkflowTag(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName,
    val workflowId: WorkflowId,
) : WorkflowTagEngineMessage()

@Serializable
data class GetWorkflowIds(
    override val workflowTag: WorkflowTag,
    override val workflowName: WorkflowName,
    val clientName: ClientName
) : WorkflowTagEngineMessage()
