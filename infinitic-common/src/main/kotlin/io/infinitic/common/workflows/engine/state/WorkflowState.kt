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

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MessageId
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class WorkflowState(
    /*
    Name of client that dispatched this workflow
     */
    val clientName: ClientName,

    /*
    Does the client synchronously waits for the output?
     */
    val clientWaiting: Boolean,

    /*
    Id of last received message (used to ensure idempotency)
     */
    var lastMessageId: MessageId,

    /*
    Id of this workflow instance
     */
    val workflowId: WorkflowId,

    /*
    Id of parent's workflow (if any)
     */
    val parentWorkflowId: WorkflowId? = null,

    /*
    Workflow's name (used wy worker to instantiate)
     */
    val workflowName: WorkflowName,

    /*
    Instance's options defined when dispatched
     */
    val workflowOptions: WorkflowOptions,

    /*
    Instance's meta defined when dispatched
     */
    val workflowMeta: WorkflowMeta,

    /*
    Id of WorkflowTask currently running (max one at a time)
     */
    var runningWorkflowTaskId: WorkflowTaskId? = null,

    /*
    Incremental index counting WorkflowTasks (used as an index for instance's state)
     */
    var workflowTaskIndex: WorkflowTaskIndex = WorkflowTaskIndex(0),

    /*
    Methods currently running. Once completed this data can be deleted to limit memory usage
     */
    val methodRuns: MutableList<MethodRun>,

    /*
    Current (last) hash of instance's properties. hash is used as an index to actual value
     */
    val currentPropertiesNameHash: MutableMap<PropertyName, PropertyHash> = mutableMapOf(),

    /*
    Store containing values of past and current values of properties
    (past values are useful when replaying WorkflowTask)
     */
    var propertiesHashValue: MutableMap<PropertyHash, PropertyValue> = mutableMapOf(),

    /*
    Messages received while a WorkflowTask is still running.
    They can not be handled immediately, so are stored in this buffer
     */
    val bufferedMessages: MutableList<WorkflowEngineMessage> = mutableListOf(),

    /*
    In some situations, we know that multiples branches must be processed.
    As WorkflowTask handles branch one by one, we orderly buffer these branches here
    (it happens when a workflowTask decide to launch more than one async branch,
    or when more than one branch' steps are completed by the same message)
     */
    val bufferedCommands: MutableList<CommandId> = mutableListOf()
) {
    companion object {
        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
        fun fromByteBuffer(bytes: ByteBuffer) = fromByteArray(bytes.array())
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())
}
