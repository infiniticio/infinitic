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

package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.CommandStatus.Canceled
import io.infinitic.common.workflows.data.commands.CommandStatus.Completed
import io.infinitic.common.workflows.data.commands.CommandStatus.CurrentlyFailed
import io.infinitic.common.workflows.data.commands.CommandStatus.Running
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.exceptions.thisShouldNotHappen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.Int.Companion.MAX_VALUE

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
sealed class Step {
    @JsonIgnore fun isTerminated() = isTerminatedAt(WorkflowTaskIndex(MAX_VALUE))
    fun status() = statusAt(WorkflowTaskIndex(MAX_VALUE))

    @JsonIgnore abstract fun isTerminatedAt(index: WorkflowTaskIndex): Boolean
    abstract fun statusAt(index: WorkflowTaskIndex): StepStatus

    /**
     * hash function excludes commandStatus and provide a hopefully unique hash linked to the structure of the step
     */
    abstract fun hash(): StepHash

    @Serializable
    @SerialName("Step.Id")
    data class Id(
        val commandId: CommandId
    ) : Step() {
        @JsonIgnore var commandStatus: CommandStatus = Running

        companion object {
            fun from(newCommand: NewCommand) = Id(newCommand.commandId)
            fun from(pastCommand: PastCommand) = Id(pastCommand.commandId).apply { commandStatus = pastCommand.commandStatus }

            @JsonCreator @JvmStatic
            // This is needed for Jackson deserialization, CommandId being an inline type
            fun new(commandId: String) = Id(CommandId(commandId))
        }

        override fun hash() = StepHash(SerializedData.from(commandId).hash())

        @JsonIgnore override fun isTerminatedAt(index: WorkflowTaskIndex) = when (statusAt(index)) {
            is StepStatus.Waiting -> false
            is StepStatus.OngoingFailure -> true
            is StepStatus.Completed -> true
            is StepStatus.Canceled -> true
            is StepStatus.Failed -> thisShouldNotHappen()
        }

        override fun statusAt(index: WorkflowTaskIndex) = when (val status = commandStatus) {
            is Running -> StepStatus.Waiting
            is Completed -> when (index >= status.completionWorkflowTaskIndex) {
                true -> StepStatus.Completed(status.returnValue, status.completionWorkflowTaskIndex)
                false -> StepStatus.Waiting
            }
            is Canceled -> when (index >= status.cancellationWorkflowTaskIndex) {
                true -> StepStatus.Canceled(commandId, status.cancellationWorkflowTaskIndex)
                false -> StepStatus.Waiting
            }
            is CurrentlyFailed -> when (index >= status.failureWorkflowTaskIndex) {
                true -> StepStatus.OngoingFailure(commandId, status.failureWorkflowTaskIndex)
                false -> StepStatus.Waiting
            }
        }
    }

    @Serializable
    @SerialName("Step.And")
    data class And(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore override fun isTerminatedAt(index: WorkflowTaskIndex) =
            this.steps.all { it.isTerminatedAt(index) }

        override fun statusAt(index: WorkflowTaskIndex): StepStatus {
            val statuses = steps.map { it.statusAt(index) }

            // if at least one step is canceled or ongoingFailure, then And(...steps) is the first of them
            val firstTerminated = statuses
                .filter { it is StepStatus.OngoingFailure && it is StepStatus.Canceled }
                .minByOrNull {
                    when (it) {
                        is StepStatus.OngoingFailure -> it.failureWorkflowTaskIndex
                        is StepStatus.Canceled -> it.cancellationWorkflowTaskIndex
                        is StepStatus.Completed, is StepStatus.Failed, is StepStatus.Waiting -> thisShouldNotHappen()
                    }
                }
            if (firstTerminated != null) return firstTerminated

            // if at least one step is ongoing, then And(...steps) is ongoing
            if (statuses.any { it is StepStatus.Waiting }) return StepStatus.Waiting

            // if all steps are completed, then And(...steps) is completed
            if (statuses.all { it is StepStatus.Completed }) {
                val maxIndex = statuses.maxOf { (it as StepStatus.Completed).completionWorkflowTaskIndex }
                val results = statuses.map { (it as StepStatus.Completed).returnValue.value() }

                return StepStatus.Completed(ReturnValue.from(results), maxIndex)
            }

            thisShouldNotHappen()
        }
    }

    @Serializable
    @SerialName("Step.Or")
    data class Or(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore override fun isTerminatedAt(index: WorkflowTaskIndex) =
            this.steps.any { it.isTerminatedAt(index) }

        override fun statusAt(index: WorkflowTaskIndex): StepStatus {
            val statuses = steps.map { it.statusAt(index) }

            // if at least one step is completed, then Or(...steps) is the first completed
            val firstCompleted = statuses
                .filterIsInstance<StepStatus.Completed>()
                .minByOrNull { it.completionWorkflowTaskIndex }
            if (firstCompleted != null) return firstCompleted

            // if at least one step is ongoing, then Or(...steps) is ongoing
            if (statuses.any { it is StepStatus.Waiting }) return StepStatus.Waiting

            // all steps are neither completed, neither ongoing => canceled, failed based on last one
            val lastTerminated = statuses.maxByOrNull {
                when (it) {
                    is StepStatus.OngoingFailure -> it.failureWorkflowTaskIndex
                    is StepStatus.Canceled -> it.cancellationWorkflowTaskIndex
                    is StepStatus.Completed, is StepStatus.Failed, is StepStatus.Waiting -> thisShouldNotHappen()
                }
            }
            if (lastTerminated != null) return lastTerminated

            thisShouldNotHappen()
        }
    }

    /*
     * Used in engine to update a step after having cancelled or completed a command
     */
    fun updateWith(commandId: CommandId, commandStatus: CommandStatus): Step {
        when (this) {
            is Id -> if (this.commandId == commandId) this.commandStatus = commandStatus
            is And -> steps = steps.map { it.updateWith(commandId, commandStatus) }
            is Or -> steps = steps.map { it.updateWith(commandId, commandStatus) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): Step {
        when (this) {
            is Id -> Unit
            is And -> steps = steps.map { it.resolveOr() }
            is Or -> steps = when (isTerminated()) {
                true -> listOf(steps.first { it.isTerminated() }.resolveOr())
                false -> steps.map { s -> s.resolveOr() }
            }
        }
        return this
    }

    private fun compose(): Step {
        when (this) {
            is Id -> Unit
            is And -> while (steps.any { it is And || (it is Or && it.steps.count() == 1) }) {
                steps = steps.fold(mutableListOf()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.steps); l }
                        is Or -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
                    }
                }
            }
            is Or -> while (steps.any { it is Or || (it is And && it.steps.count() == 1) }) {
                steps = steps.fold(mutableListOf()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
                        is Or -> { l.addAll(s.steps); l }
                    }
                }
            }
        }
        return this
    }
}
