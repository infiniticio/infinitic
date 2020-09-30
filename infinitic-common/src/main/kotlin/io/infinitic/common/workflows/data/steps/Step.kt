// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.CommandStatusCanceled
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex
import kotlin.Int.Companion.MAX_VALUE
import kotlin.Int.Companion.MIN_VALUE

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Step.Id::class, name = "ID"),
    JsonSubTypes.Type(value = Step.And::class, name = "AND"),
    JsonSubTypes.Type(value = Step.Or::class, name = "OR")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Step {
    @JsonIgnore
    fun isTerminated() = isTerminatedAtMessageIndex(WorkflowMessageIndex(MAX_VALUE))

    fun stepStatus() = stepStatusAtMessageIndex(WorkflowMessageIndex(MAX_VALUE))

    abstract fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex): Boolean

    abstract fun stepStatusAtMessageIndex(index: WorkflowMessageIndex): StepStatus

    /*
     * hash function is defined to exclude commandStatus and provide a hopefully unique hash linked to the structure of the step
     */
    abstract fun hash(): StepHash

    data class Id(
        val commandId: CommandId,
        var commandStatus: CommandStatus
    ) : Step() {

        override fun hash() = StepHash(SerializedData.from(commandId).hash())

        @JsonIgnore
        override fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex) = when (stepStatusAtMessageIndex(index)) {
            is StepStatusCanceled -> true
            is StepStatusCompleted -> true
            is StepStatusOngoing -> false
        }

        override fun stepStatusAtMessageIndex(index: WorkflowMessageIndex) = when (commandStatus) {
            is CommandStatusOngoing -> StepStatusOngoing
            is CommandStatusCompleted -> with(commandStatus as CommandStatusCompleted) {
                when (index >= this.completionWorkflowMessageIndex) {
                    true -> StepStatusCompleted(StepOutput(this.completionResult.data), this.completionWorkflowMessageIndex)
                    false -> StepStatusOngoing
                }
            }
            is CommandStatusCanceled -> with(commandStatus as CommandStatusCanceled) {
                when (index >= this.cancellationWorkflowMessageIndex) {
                    true -> StepStatusCanceled(StepOutput(this.cancellationResult.data), this.cancellationWorkflowMessageIndex)
                    false -> StepStatusOngoing
                }
            }
        }

        companion object {
            fun from(newCommand: NewCommand) = Id(newCommand.commandId, CommandStatusOngoing)
            fun from(pastCommand: PastCommand) = Id(pastCommand.commandId, pastCommand.commandStatus)
        }
    }

    data class And(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore
        override fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex) = this.steps.all { s -> s.isTerminatedAtMessageIndex(index) }

        override fun stepStatusAtMessageIndex(index: WorkflowMessageIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAtMessageIndex(index) }
            if (statuses.any { it is StepStatusOngoing }) return StepStatusOngoing

            val results = statuses.map {
                when (it) {
                    is StepStatusOngoing -> throw Exception("This should not happen")
                    is StepStatusCompleted -> it.completionResult
                    is StepStatusCanceled -> it.cancellationResult
                }
            }
            val maxIndex = statuses.map {
                when (it) {
                    is StepStatusOngoing -> WorkflowMessageIndex(MIN_VALUE)
                    is StepStatusCompleted -> it.completionWorkflowMessageIndex
                    is StepStatusCanceled -> it.cancellationWorkflowMessageIndex
                }
            }.max()!!

            if (statuses.all { it is StepStatusCompleted }) return StepStatusCompleted(StepOutput(results.map { it.data }), maxIndex)

            return StepStatusCanceled(StepOutput(results.map { it.data }), maxIndex)
        }
    }

    data class Or(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore
        override fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex) = this.steps.any { s -> s.isTerminatedAtMessageIndex(index) }

        override fun stepStatusAtMessageIndex(index: WorkflowMessageIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAtMessageIndex(index) }
            // if all steps are ongoing then returns StepStatusOngoing
            if (statuses.all { it is StepStatusOngoing }) return StepStatusOngoing
            // find first step not ongoing
            val minStep = statuses.minBy {
                when (it) {
                    is StepStatusOngoing -> WorkflowMessageIndex(MAX_VALUE)
                    is StepStatusCompleted -> it.completionWorkflowMessageIndex
                    is StepStatusCanceled -> it.cancellationWorkflowMessageIndex
                }
            }!!

            return when (minStep) {
                is StepStatusOngoing -> throw RuntimeException("This should not happen")
                is StepStatusCompleted -> StepStatusCompleted(minStep.completionResult, minStep.completionWorkflowMessageIndex)
                is StepStatusCanceled -> StepStatusCanceled(minStep.cancellationResult, minStep.cancellationWorkflowMessageIndex)
            }
        }
    }

    /*
     * Used in engine to update a step after having cancelled or completed a command
     */
    fun update(commandId: CommandId, commandStatus: CommandStatus): Step {
        when (this) {
            is Id -> if (this.commandId == commandId) this.commandStatus = commandStatus
            is And -> this.steps = this.steps.map { s -> s.update(commandId, commandStatus) }
            is Or -> this.steps = this.steps.map { s -> s.update(commandId, commandStatus) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): Step {
        when (this) {
            is Id -> Unit
            is And -> this.steps = this.steps.map { s -> s.resolveOr() }
            is Or ->
                this.steps =
                    if (this.isTerminated())
                        listOf(this.steps.first { s -> s.isTerminated() }.resolveOr())
                    else
                        this.steps.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): Step {
        when (this) {
            is Id -> Unit
            is And -> while (this.steps.any { s -> s is And || (s is Or && s.steps.count() == 1) }) {
                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.steps); l }
                        is Or -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.steps.any { s -> s is Or || (s is And && s.steps.count() == 1) }) {
                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
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
