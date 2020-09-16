package io.infinitic.workflowManager.common.data.instructions

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.workflowManager.common.data.commands.CommandHash
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.commands.CommandStatusCanceled
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandType
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.NewStep
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.steps.StepHash
import io.infinitic.workflowManager.common.data.steps.StepStatus
import io.infinitic.workflowManager.common.data.steps.StepStatusCanceled
import io.infinitic.workflowManager.common.data.steps.StepStatusCompleted
import io.infinitic.workflowManager.common.data.steps.StepStatusOngoing
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowChangeCheckMode

sealed class PastInstruction(
    open val stringPosition: StringPosition
) {
    abstract fun isTerminated(): Boolean
}

data class PastCommand(
    override val stringPosition: StringPosition,
    val commandType: CommandType,
    val commandId: CommandId,
    val commandHash: CommandHash,
    val commandSimpleName: CommandSimpleName,
    var commandStatus: CommandStatus
) : PastInstruction(stringPosition) {
    @JsonIgnore
    override fun isTerminated() = this.commandStatus is CommandStatusCompleted || this.commandStatus is CommandStatusCanceled

    fun isSimilarTo(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean =
        newCommand.commandStringPosition == stringPosition &&
            when (mode) {
                WorkflowChangeCheckMode.NONE ->
                    true
                WorkflowChangeCheckMode.SIMPLE_NAME_ONLY ->
                    newCommand.commandType == commandType && newCommand.commandSimpleName == commandSimpleName
                WorkflowChangeCheckMode.ALL ->
                    newCommand.commandHash == commandHash
            }
}

data class PastStep(
    override val stringPosition: StringPosition,
    val step: Step,
    val stepHash: StepHash,
    var stepStatus: StepStatus = StepStatusOngoing(),
    var workflowPropertiesAfterCompletion: Properties? = null,
    var completionWorkflowEventIndex: WorkflowEventIndex? = null
) : PastInstruction(stringPosition) {

    @JsonIgnore
    override fun isTerminated() = this.stepStatus is StepStatusCompleted || this.stepStatus is StepStatusCanceled


    fun completeCommand(commandId: CommandId, properties: Properties): Boolean {
        return complete(commandId, properties)
    }

    private fun complete(commandId: CommandId, properties: Properties): Boolean {
        if (! isTerminated()) {
            step.complete(commandId)
            if (step.isCompleted()) {
                workflowPropertiesAfterCompletion = properties.copy()
                return true
            }
        }
        return false
    }

    fun isSimilarTo(newStep: NewStep) = newStep.stepHash == stepHash
}
