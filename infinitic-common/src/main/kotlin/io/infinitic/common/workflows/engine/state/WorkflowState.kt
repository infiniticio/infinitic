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

import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.PastCommand
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
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.workflows.WorkflowOptions
import kotlinx.serialization.Serializable

@Serializable @AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowState(
    /**
     * Infinitic version
     */
    @AvroDefault("0.9.0") // last version without this field
    val version: String = io.infinitic.version,

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
     * In some situations, we know that multiples branches must be processed.
     * As WorkflowTask handles branch one by one, we orderly buffer these branches here
     * (it happens when a terminated command completes currentStep on multiple MethodRuns)
     */
    val runningTerminatedCommands: MutableList<CommandId> = mutableListOf(),

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
     * They can not be handled immediately, so we store them in this buffer
     */
    val messagesBuffer: MutableList<WorkflowEngineMessage> = mutableListOf(),
) {
    companion object {
        fun fromByteArray(bytes: ByteArray) = AvroSerDe.readBinary(bytes, serializer())
    }

    fun toByteArray() = AvroSerDe.writeBinary(this, serializer())

    fun hasSignalAlreadyBeenReceived(signalId: SignalId) = methodRuns.any { methodRun ->
        methodRun.pastCommands.any {
            it.commandStatus is CommandStatus.Completed && (it.commandStatus as CommandStatus.Completed).signalId == signalId
        }
    }

    fun getRunningMethodRun(): MethodRun = methodRuns.first { it.methodRunId == runningMethodRunId }

    fun getMethodRun(methodRunId: MethodRunId) = methodRuns.firstOrNull { it.methodRunId == methodRunId }

    fun getPastCommand(commandId: CommandId, methodRun: MethodRun): PastCommand =
        methodRun.getPastCommand(commandId)
            // if we do not find in this methodRun, then search within others
            ?: methodRuns.map { it.getPastCommand(commandId) }.firstOrNull { it != null }
            // methodRun should not be deleted if a step is still running
            ?: thisShouldNotHappen()

    fun removeMethodRun(methodRun: MethodRun) {
        methodRuns.remove(methodRun)

        // clean receivingChannels once deleted
        receivingChannels.removeAll { it.methodRunId == methodRun.methodRunId }

        // clean properties
        removeUnusedPropertyHash()
    }

    fun removeMethodRuns() {
        methodRuns.clear()

        // clean receivingChannels once deleted
        receivingChannels.clear()

        // clean properties
        removeUnusedPropertyHash()
    }

    /**
     * After completion or cancellation of methodRuns, we can clean up the properties not used anymore
     */
    private fun removeUnusedPropertyHash() {
        // set of all hashes still in use
        val propertyHashes = mutableSetOf<PropertyHash>()

        methodRuns.forEach { methodRun ->
            methodRun.pastSteps.forEach { pastStep ->
                pastStep.propertiesNameHashAtTermination?.forEach { propertyHashes.add(it.value) }
            }
            methodRun.propertiesNameHashAtStart.forEach { propertyHashes.add(it.value) }
        }
        currentPropertiesNameHash.forEach { propertyHashes.add(it.value) }

        // remove all propertyHashValue not in use anymore
        propertiesHashValue.keys
            .filter { it !in propertyHashes }
            .forEach { propertiesHashValue.remove(it) }
    }
}
