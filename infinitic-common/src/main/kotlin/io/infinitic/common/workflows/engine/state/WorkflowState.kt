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

package io.infinitic.common.workflows.engine.state

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowStatus
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.thisShouldNotHappen
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowState(
    /**
     * Workflow's status
     */
    var workflowStatus: WorkflowStatus,

    /**
     * Id of last handled message (used to ensure idempotency)
     */
    var lastMessageId: MessageId,

    /**
     * Id of this workflow instance
     */
    val workflowId: WorkflowId,

    /**
     * Workflow's name (used wy worker to instantiate)
     */
    val workflowName: WorkflowName,

    /**
     * Instance's tags defined when dispatched
     */
    val workflowTags: Set<WorkflowTag>,

    /**
     * Instance's options defined when dispatched
     */
    val workflowOptions: WorkflowOptions,

    /**
     * Instance's meta defined when dispatched
     */
    val workflowMeta: WorkflowMeta,

    /**
     * Id of WorkflowTask currently running
     */
    var runningWorkflowTaskId: TaskId? = null,

    /**
     * MethodRunId of WorkflowTask currently running
     */
    var runningMethodRunId: MethodRunId? = null,

    /**
     * Position of the step that triggered WorkflowTask currently running
     */
    var runningMethodRunPosition: MethodRunPosition? = null,

    /**
     * Instant when WorkflowTask currently running was triggered
     */
    var runningWorkflowTaskInstant: MillisInstant? = null,

    /**
     * Incremental index counting WorkflowTasks (used as an index for instance's state)
     */
    var workflowTaskIndex: WorkflowTaskIndex = WorkflowTaskIndex(0),

    /**
     * Methods currently running. Once completed this data can be deleted to limit memory usage
     */
    val methodRuns: MutableList<MethodRun>,

    /**
     * Ordered list of channels currently receiving
     */
    val receivingChannels: MutableList<ReceivingChannel> = mutableListOf(),

    /**
     * Current (last) hash of instance's properties. hash is used as an index to actual value
     */
    val currentPropertiesNameHash: MutableMap<PropertyName, PropertyHash> = mutableMapOf(),

    /**
     * Store containing values of past and current values of properties
     * (past values are useful when replaying WorkflowTask)
     */
    var propertiesHashValue: MutableMap<PropertyHash, PropertyValue> = mutableMapOf(),

    /**
     * Messages received while a WorkflowTask is still running.
     * They can not be handled immediately, so are stored in this buffer
     */
    val bufferedMessages: MutableList<WorkflowEngineMessage> = mutableListOf(),

    /**
     * In some situations, we know that multiples branches must be processed.
     * As WorkflowTask handles branch one by one, we orderly buffer these branches here
     * (it happens when a workflowTask decide to launch more than one async branch,
     * or when more than one branch' steps are completed by the same message)
     */
    val bufferedCommands: MutableList<CommandId> = mutableListOf()
) {
    companion object {
        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())

    fun getRunningMethodRun(): MethodRun = methodRuns.first { it.methodRunId == runningMethodRunId }

    fun getMethodRun(methodRunId: MethodRunId) = methodRuns.firstOrNull() { it.methodRunId == methodRunId }

    fun getMainMethodRun() = methodRuns.find { it.methodRunId.id == workflowId.id }

    /**
     * true if the current workflow task is on main path
     */
    fun isRunningWorkflowTaskOnMainPath(): Boolean {
        val p = runningMethodRunPosition ?: throw thisShouldNotHappen()

        return p.isOnMainPath() &&
            getRunningMethodRun().getCommandByPosition(p)?.commandType != CommandType.START_ASYNC
    }
}
