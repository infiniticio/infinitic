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

import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.commands.CommandCanceled
import io.infinitic.common.workflows.data.commands.CommandCompleted
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandOngoing
import io.infinitic.common.workflows.data.commands.CommandOngoingFailure
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.exceptions.thisShouldNotHappen
import kotlinx.serialization.Serializable
import kotlin.Int.Companion.MAX_VALUE

@Serializable
sealed class Step {
    fun isTerminated() = isTerminatedAt(WorkflowTaskIndex(MAX_VALUE))
    fun stepStatus() = stepStatusAt(WorkflowTaskIndex(MAX_VALUE))

    abstract fun isTerminatedAt(index: WorkflowTaskIndex): Boolean
    abstract fun stepStatusAt(index: WorkflowTaskIndex): StepStatus

    /*
     * hash function is defined to exclude commandStatus and provide a hopefully unique hash linked to the structure of the step
     */
    abstract fun hash(): StepHash

    @Serializable
    data class Id(
        val commandId: CommandId,
        var commandStatus: CommandStatus
    ) : Step() {

        override fun hash() = StepHash(SerializedData.from(commandId).hash())

        override fun isTerminatedAt(index: WorkflowTaskIndex) = when (stepStatusAt(index)) {
            is StepOngoing -> false
            is StepOngoingFailure -> true
            is StepCompleted -> true
            is StepCanceled -> true
            is StepFailed -> thisShouldNotHappen()
        }

        override fun stepStatusAt(index: WorkflowTaskIndex) = when (val status = commandStatus) {
            is CommandOngoing -> StepOngoing
            is CommandCompleted -> when (index >= status.completionWorkflowTaskIndex) {
                true -> StepCompleted(StepReturnValue.from(status.returnValue.get()), status.completionWorkflowTaskIndex)
                false -> StepOngoing
            }
            is CommandCanceled -> when (index >= status.cancellationWorkflowTaskIndex) {
                true -> StepCanceled(commandId, status.cancellationWorkflowTaskIndex)
                false -> StepOngoing
            }
            is CommandOngoingFailure -> when (index >= status.failureWorkflowTaskIndex) {
                true -> StepOngoingFailure(commandId, status.failureWorkflowTaskIndex)
                false -> StepOngoing
            }
        }

        companion object {
            fun from(newCommand: NewCommand) = Id(newCommand.commandId, CommandOngoing)
            fun from(pastCommand: PastCommand) = Id(pastCommand.commandId, pastCommand.commandStatus)
        }
    }

    @Serializable
    data class And(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        override fun isTerminatedAt(index: WorkflowTaskIndex) =
            this.steps.all { it.isTerminatedAt(index) }

        override fun stepStatusAt(index: WorkflowTaskIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAt(index) }

            // if at least one step is canceled or ongoingFailure, then And(...steps) is the first of them
            val firstTerminated = statuses
                .filter { it is StepOngoingFailure && it is StepCanceled }
                .minByOrNull {
                    when (it) {
                        is StepOngoingFailure -> it.failureWorkflowTaskIndex
                        is StepCanceled -> it.cancellationWorkflowTaskIndex
                        is StepCompleted, is StepFailed, is StepOngoing -> thisShouldNotHappen()
                    }
                }
            if (firstTerminated != null) return firstTerminated

            // if at least one step is ongoing, then And(...steps) is ongoing
            if (statuses.any { it is StepOngoing }) return StepOngoing

            // if all steps are completed, then And(...steps) is completed
            if (statuses.all { it is StepCompleted }) {
                val maxIndex = statuses.maxOf { (it as StepCompleted).completionWorkflowTaskIndex }
                val results = statuses.map { (it as StepCompleted).returnValue.get() }

                return StepCompleted(StepReturnValue.from(results), maxIndex)
            }

            thisShouldNotHappen()
        }
    }

    @Serializable
    data class Or(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        override fun isTerminatedAt(index: WorkflowTaskIndex) =
            this.steps.any { it.isTerminatedAt(index) }

        override fun stepStatusAt(index: WorkflowTaskIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAt(index) }

            // if at least one step is completed, then Or(...steps) is the first completed
            val firstCompleted = statuses
                .filterIsInstance<StepCompleted>()
                .minByOrNull { it.completionWorkflowTaskIndex }
            if (firstCompleted != null) return firstCompleted

            // if at least one step is ongoing, then Or(...steps) is ongoing
            if (statuses.any { it is StepOngoing }) return StepOngoing

            // all steps are neither completed, neither ongoing => canceled, failed based on last one
            val lastTerminated = statuses.maxByOrNull {
                when (it) {
                    is StepOngoingFailure -> it.failureWorkflowTaskIndex
                    is StepCanceled -> it.cancellationWorkflowTaskIndex
                    is StepCompleted, is StepFailed, is StepOngoing -> thisShouldNotHappen()
                }
            }
            if (lastTerminated != null) return lastTerminated

            thisShouldNotHappen()
        }
    }

    /*
     * Used in engine to update a step after having cancelled or completed a command
     */
    fun update(commandId: CommandId, commandStatus: CommandStatus): Step {
        when (this) {
            is Id -> if (this.commandId == commandId) this.commandStatus = commandStatus
            is And -> steps = steps.map { it.update(commandId, commandStatus) }
            is Or -> steps = steps.map { it.update(commandId, commandStatus) }
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
