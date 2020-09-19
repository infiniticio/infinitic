package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.commands.CommandStatusCanceled
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
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
            is CommandStatusOngoing -> StepStatusOngoing()
            is CommandStatusCompleted -> with(commandStatus as CommandStatusCompleted) {
                when (index >= this.completionWorkflowMessageIndex) {
                    true -> StepStatusCompleted(this.result, this.completionWorkflowMessageIndex)
                    false -> StepStatusOngoing()
                }
            }
            is CommandStatusCanceled -> with(commandStatus as CommandStatusCanceled) {
                when (index >= this.cancellationWorkflowMessageIndex) {
                    true -> StepStatusCanceled(this.result, this.cancellationWorkflowMessageIndex)
                    false -> StepStatusOngoing()
                }
            }
        }

        companion object {
            fun from(newCommand: NewCommand) = Id(newCommand.commandId, CommandStatusOngoing())
            fun from(pastCommand: PastCommand) = Id(pastCommand.commandId, pastCommand.commandStatus)
        }
    }

    data class And(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore
        override fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex) = this.steps.all { s -> s.isTerminatedAtMessageIndex(index) }

        override fun stepStatusAtMessageIndex(index: WorkflowMessageIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAtMessageIndex(index) }
            if (statuses.any { it is StepStatusOngoing }) return StepStatusOngoing()

            val results = statuses.map {
                when (it) {
                    is StepStatusOngoing -> null
                    is StepStatusCompleted -> it.result
                    is StepStatusCanceled -> it.result
                }
            }
            val maxIndex = statuses.map {
                when (it) {
                    is StepStatusOngoing -> WorkflowMessageIndex(MIN_VALUE)
                    is StepStatusCompleted -> it.completionWorkflowMessageIndex
                    is StepStatusCanceled -> it.cancellationWorkflowMessageIndex
                }
            }.max()!!

            if (statuses.all { it is StepStatusCompleted }) return StepStatusCompleted(results, maxIndex)

            return StepStatusCanceled(results, maxIndex)
        }
    }

    data class Or(var steps: List<Step>) : Step() {

        override fun hash() = StepHash(SerializedData.from(steps.map { it.hash() }).hash())

        @JsonIgnore
        override fun isTerminatedAtMessageIndex(index: WorkflowMessageIndex) = this.steps.any { s -> s.isTerminatedAtMessageIndex(index) }

        override fun stepStatusAtMessageIndex(index: WorkflowMessageIndex): StepStatus {
            val statuses = steps.map { it.stepStatusAtMessageIndex(index) }
            // if all steps are ongoing then returns StepStatusOngoing
            if (statuses.all { it is StepStatusOngoing }) return StepStatusOngoing()
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
                is StepStatusCompleted -> StepStatusCompleted(minStep.result, minStep.completionWorkflowMessageIndex)
                is StepStatusCanceled -> StepStatusCanceled(minStep.result, minStep.cancellationWorkflowMessageIndex)
            }
        }
    }

    /*
     * Used in engine to update a step after having cancelled or completed a command
     */
    fun updateWith(pastCommand: PastCommand): Step {
        when (this) {
            is Id -> if (this.commandId == pastCommand.commandId) this.commandStatus = pastCommand.commandStatus
            is And -> this.steps = this.steps.map { s -> s.updateWith(pastCommand) }
            is Or -> this.steps = this.steps.map { s -> s.updateWith(pastCommand) }
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
