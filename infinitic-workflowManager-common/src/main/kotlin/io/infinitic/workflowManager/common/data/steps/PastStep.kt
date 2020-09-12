package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.events.EventId
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.instructions.StringPosition
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex

class PastStep(
    override val stringPosition: StringPosition,
    val step: Step,
    val stepHash: StepHash,
    var stepStatus: StepStatus,
    var workflowPropertiesAfterCompletion: Properties,
    var completedFromWorkflowTaskIndex: WorkflowTaskIndex
) : PastInstruction(stringPosition) {
//    @JsonIgnore fun isCompleted() = step.isCompleted()
//
//    fun completeTask(taskId: TaskId, properties: Properties): Boolean {
//        return complete(CommandId(taskId), properties)
//    }
//
//    fun completeChildWorkflow(workflowId: WorkflowId, properties: Properties): Boolean {
//        return complete(CommandId(workflowId), properties)
//    }
//
//    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
//        return complete(CommandId(delayId), properties)
//    }
//
//    fun completeEvent(eventId: EventId, properties: Properties): Boolean {
//        return complete(CommandId(eventId), properties)
//    }

//    private fun complete(commandId: CommandId, properties: Properties): Boolean {
//        if (! isCompleted()) {
//            step.complete(commandId)
//            if (step.isCompleted()) {
//                workflowPropertiesAfterCompletion = properties.copy()
//                return true
//            }
//        }
//        return false
//    }

    fun isSimilarTo(newStep: NewStep) = newStep.stepHash == stepHash
}
