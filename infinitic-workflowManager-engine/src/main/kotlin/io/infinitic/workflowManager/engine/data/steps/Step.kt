package io.infinitic.workflowManager.engine.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.engine.data.DelayId
import io.infinitic.workflowManager.engine.data.EventId
import io.infinitic.workflowManager.engine.data.WorkflowId
import io.infinitic.workflowManager.engine.data.commands.CommandId
import io.infinitic.workflowManager.engine.data.properties.Properties

data class Step(
    val stepHash: StepHash,
    val criterion: StepCriterion,
    var propertiesAfterCompletion: Properties
) {
    @JsonIgnore
    fun isCompleted() = criterion.isCompleted()

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
            criterion.complete(commandId)
            if (criterion.isCompleted()) {
                propertiesAfterCompletion = properties.copy()
                return true
            }
        }
        return false
    }
}
