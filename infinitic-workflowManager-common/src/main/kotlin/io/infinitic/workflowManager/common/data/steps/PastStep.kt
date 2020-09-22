package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.methodRuns.MethodPosition
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex

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
