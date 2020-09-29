package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodPosition
import io.infinitic.common.workflows.data.properties.Properties
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex

data class PastStep(
    val stepPosition: MethodPosition,
    val step: Step,
    val stepHash: StepHash,
    var stepStatus: StepStatus = StepStatusOngoing,
    var propertiesAtTermination: Properties? = null,
    var messageIndexAtTermination: WorkflowMessageIndex? = null
) {

    @JsonIgnore
    fun isTerminated() = stepStatus is StepStatusCompleted || stepStatus is StepStatusCanceled

    fun terminateBy(pastCommand: PastCommand, properties: Properties): Boolean {
        if (isTerminated()) return false

        step.update(pastCommand.commandId, pastCommand.commandStatus)
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
