package io.infinitic.workflowManager.common.data.instructions

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowChangeCheckMode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PastCommand::class, name = "PAST_COMMAND"),
    JsonSubTypes.Type(value = PastStep::class, name = "PAST_STEP")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class PastInstruction(
    open val stringPosition: StringPosition
) {
    abstract fun isTerminated(): Boolean
}

data class PastCommand(
    @JsonProperty("position")
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
    @JsonProperty("position")
    override val stringPosition: StringPosition,
    val step: Step,
    val stepHash: StepHash,
    var stepStatus: StepStatus = StepStatusOngoing,
    var propertiesAtTermination: Properties? = null,
    var messageIndexAtTermination: WorkflowMessageIndex? = null
) : PastInstruction(stringPosition) {

    @JsonIgnore
    override fun isTerminated() = stepStatus is StepStatusCompleted || stepStatus is StepStatusCanceled

    fun terminateBy(pastCommand: PastCommand, properties: Properties): Boolean {
        if (isTerminated()) return false

        step.updateWith(pastCommand)
        stepStatus = step.stepStatus()
        return when (stepStatus) {
            is StepStatusOngoing -> false
            is StepStatusCanceled, is StepStatusCompleted -> {
                propertiesAtTermination = properties
                true
            }
        }
    }

    fun isSimilarTo(newStep: NewStep) = newStep.stepHash == stepHash
}
