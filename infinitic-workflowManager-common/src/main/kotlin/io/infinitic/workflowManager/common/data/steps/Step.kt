package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.data.SerializedData
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandStatusCanceled
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex
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
    abstract fun isTerminated(index: WorkflowEventIndex): Boolean

    abstract fun stepStatus(index: WorkflowEventIndex): StepStatus

    fun hash() = StepHash(SerializedData.from(this).hash())

    data class Id(
        val commandId: CommandId,
        @JsonIgnore private val funStepStatus: (index: WorkflowEventIndex) -> StepStatus
    ) : Step() {

        override fun isTerminated(index: WorkflowEventIndex) = when(stepStatus(index)) {
            is StepStatusCanceled -> true
            is StepStatusCompleted -> true
            is StepStatusOngoing -> false
        }

        override fun stepStatus(index: WorkflowEventIndex) = funStepStatus(index)

        companion object {
            fun from(newCommand: NewCommand) = Id(newCommand.commandId) { StepStatusOngoing() }

            fun from(pastCommand: PastCommand) = Id(pastCommand.commandId) { index ->
                when (val status = pastCommand.commandStatus) {
                    is CommandStatusOngoing -> {
                        StepStatusOngoing()
                    }
                    is CommandStatusCanceled ->
                        if (index < status.cancellationWorkflowEventIndex) {
                            StepStatusOngoing()
                        } else {
                            StepStatusCanceled(status.result, status.cancellationWorkflowEventIndex)
                        }
                    is CommandStatusCompleted ->
                        if (index < status.completionWorkflowEventIndex) {
                            StepStatusOngoing()
                        } else {
                            StepStatusCompleted(status.result, status.completionWorkflowEventIndex)
                        }
                }
            }
        }
    }

    data class And(var steps: List<Step>) : Step() {

        override fun isTerminated(index: WorkflowEventIndex) = this.steps.all { s -> s.isTerminated(index) }

        override fun stepStatus(index: WorkflowEventIndex): StepStatus {
            val statuses = steps.map { it.stepStatus(index) }
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
                    is StepStatusOngoing -> WorkflowEventIndex(MIN_VALUE)
                    is StepStatusCompleted -> it.completionWorkflowEventIndex
                    is StepStatusCanceled -> it.cancellationWorkflowEventIndex
                }
            }.max()!!

            if (statuses.all { it is StepStatusCompleted }) return StepStatusCompleted(results, maxIndex)

            return StepStatusCanceled(results, maxIndex)
        }
    }
    data class Or(var steps: List<Step>) : Step() {

        override fun isTerminated(index: WorkflowEventIndex) = this.steps.any { s -> s.isTerminated(index) }

        override fun stepStatus(index: WorkflowEventIndex): StepStatus {
            val statuses = steps.map { it.stepStatus(index) }
            // if all steps are ongoing then returns StepStatusOngoing
            if (statuses.all { it is StepStatusOngoing }) return StepStatusOngoing()
            // find first step not ongoing
            val minStep = statuses.minBy {
                when (it) {
                    is StepStatusOngoing -> WorkflowEventIndex(MAX_VALUE)
                    is StepStatusCompleted -> it.completionWorkflowEventIndex
                    is StepStatusCanceled -> it.cancellationWorkflowEventIndex
                }
            }!!

            return when (minStep) {
                is StepStatusOngoing -> throw RuntimeException("This should not happen")
                is StepStatusCompleted -> StepStatusCompleted(minStep.result, minStep.completionWorkflowEventIndex)
                is StepStatusCanceled -> StepStatusCanceled(minStep.result, minStep.cancellationWorkflowEventIndex)
            }
        }
    }

    fun complete(commandId: CommandId): Step {
        when (this) {
            is Id -> if (this.commandId == commandId) this.isTerminated = CommandStatus.COMPLETED
            is And -> this.steps = this.steps.map { s -> s.complete(commandId) }
            is Or -> this.steps = this.steps.map { s -> s.complete(commandId) }
        }
        return this.resolveOr().compose()
    }

//    private fun resolveOr(): Step {
//        when (this) {
//            is Id -> Unit
//            is And -> this.steps = this.steps.map { s -> s.resolveOr() }
//            is Or ->
//                this.steps =
//                    if (this.isCompleted())
//                        listOf(this.steps.first { s -> s.isCompleted() }.resolveOr())
//                    else
//                        this.steps.map { s -> s.resolveOr() }
//        }
//        return this
//    }

//    private fun compose(): Step {
//        when (this) {
//            is Id -> Unit
//            is And -> while (this.steps.any { s -> s is And || (s is Or && s.steps.count() == 1) }) {
//                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
//                    return@fold when (s) {
//                        is Id -> { l.add(s); l }
//                        is And -> { l.addAll(s.steps); l }
//                        is Or -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
//                    }
//                }
//            }
//            is Or -> while (this.steps.any { s -> s is Or || (s is And && s.steps.count() == 1) }) {
//                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
//                    return@fold when (s) {
//                        is Id -> { l.add(s); l }
//                        is And -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
//                        is Or -> { l.addAll(s.steps); l }
//                    }
//                }
//            }
//        }
//        return this
//    }
}
