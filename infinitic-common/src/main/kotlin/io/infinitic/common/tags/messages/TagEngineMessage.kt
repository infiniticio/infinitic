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

package io.infinitic.common.tags.messages

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.Name
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.Serializable

@Serializable
sealed class TagEngineMessage {
    val messageId = MessageId()
    abstract val tag: Tag
    abstract val name: Name
}

@Serializable
data class SendToChannelPerTag(
    override val tag: Tag,
    override val name: WorkflowName,
    val clientName: ClientName,
    val clientWaiting: Boolean,
    val channelEventId: ChannelEventId,
    val channelName: ChannelName,
    val channelEvent: ChannelEvent,
    val channelEventTypes: Set<ChannelEventType>
) : TagEngineMessage()

@Serializable
data class RetryTaskPerTag(
    override val tag: Tag,
    override val name: TaskName,
    val methodName: MethodName?,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters?,
    val taskMeta: TaskMeta?,
    val taskOptions: TaskOptions?
) : TagEngineMessage()

@Serializable
data class CancelTaskPerTag(
    override val tag: Tag,
    override val name: TaskName,
    val taskReturnValue: MethodReturnValue
) : TagEngineMessage()

@Serializable
data class CancelWorkflowPerTag(
    override val tag: Tag,
    override val name: WorkflowName,
    val workflowOutput: MethodReturnValue
) : TagEngineMessage()

@Serializable
data class WorkflowStarted(
    override val tag: Tag,
    override val name: WorkflowName,
    val workflowId: WorkflowId,
) : TagEngineMessage()

@Serializable
data class WorkflowTerminated(
    override val tag: Tag,
    override val name: WorkflowName,
    val workflowId: WorkflowId,
) : TagEngineMessage()
