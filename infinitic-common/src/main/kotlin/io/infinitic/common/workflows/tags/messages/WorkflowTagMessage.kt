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

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.DeferredStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
sealed class WorkflowTagMessage : Message {
    val messageId = MessageId()
    abstract val emitterName: ClientName
    abstract val workflowTag: WorkflowTag
    abstract val workflowName: WorkflowName

    override fun envelope() = WorkflowTagEnvelope.from(this)
}

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class DispatchWorkflowByCustomId(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val workflowId: WorkflowId,
    val methodName: MethodName,
    val methodParameters: MethodParameters,
    val methodParameterTypes: MethodParameterTypes?,
    val workflowTags: Set<WorkflowTag>,
    val workflowMeta: WorkflowMeta,
    var parentWorkflowName: WorkflowName?,
    var parentWorkflowId: WorkflowId?,
    var parentMethodRunId: MethodRunId?,
    val clientWaiting: Boolean,
    override val emitterName: ClientName
) : WorkflowTagMessage() {
    init {
        require(workflowTag.isCustomId()) { "workflowTag must be a custom id" }
    }
}

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class SendSignalByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val channelName: ChannelName,
    @AvroName("channelSignalId")
    val signalId: SignalId,
    @AvroName("channelSignal")
    val signalData: SignalData,
    @AvroName("channelSignalTypes")
    val channelTypes: Set<ChannelType>,
    var emitterWorkflowId: WorkflowId?,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CancelWorkflowByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val reason: WorkflowCancellationReason,
    var emitterWorkflowId: WorkflowId?,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryWorkflowTaskByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RetryTasksByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val taskId: TaskId?,
    val taskStatus: DeferredStatus?,
    @SerialName("taskName") val serviceName: ServiceName?,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class CompleteTimersByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val methodRunId: MethodRunId?,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class AddTagToWorkflow(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val workflowId: WorkflowId,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class RemoveTagFromWorkflow(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    val workflowId: WorkflowId,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class GetWorkflowIdsByTag(
    override val workflowName: WorkflowName,
    override val workflowTag: WorkflowTag,
    override val emitterName: ClientName
) : WorkflowTagMessage()

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
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
    override val emitterName: ClientName
) : WorkflowTagMessage()
