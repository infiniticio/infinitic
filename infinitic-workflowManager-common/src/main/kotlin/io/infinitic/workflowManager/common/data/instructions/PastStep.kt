package io.infinitic.workflowManager.common.data.instructions

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.events.EventId
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.steps.StepHash
import io.infinitic.workflowManager.common.data.workflows.WorkflowIntegrityCheckMode

data class PastStep(
    val stepPosition: Position,
    val stepHash: StepHash,
    val step: Step,
    var propertiesAfterCompletion: Properties
) : PastInstruction(stepPosition) {
    @JsonIgnore fun isCompleted() = step.isCompleted()

    fun completeTask(taskId: TaskId, properties: Properties): Boolean {
        return complete(CommandId(taskId), properties)
    }

    fun completeChildWorkflow(workflowId: WorkflowId, properties: Properties): Boolean {
        return complete(CommandId(workflowId), properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        return complete(CommandId(delayId), properties)
    }

    fun completeEvent(eventId: EventId, properties: Properties): Boolean {
        return complete(CommandId(eventId), properties)
    }

    private fun complete(commandId: CommandId, properties: Properties): Boolean {
        if (! isCompleted()) {
            step.complete(commandId)
            if (step.isCompleted()) {
                propertiesAfterCompletion = properties.copy()
                return true
            }
        }
        return false
    }

    override fun isSimilarTo(newCommand: NewCommand, mode: WorkflowIntegrityCheckMode): Boolean {
        TODO("Not yet implemented")
    }
}
