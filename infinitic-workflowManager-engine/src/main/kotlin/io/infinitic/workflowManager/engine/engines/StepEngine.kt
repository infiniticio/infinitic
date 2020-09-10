package io.infinitic.workflowManager.engine.engines

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.events.EventId
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.workflows.WorkflowId

class StepEngine(private val step: Step) {
    @JsonIgnore
    fun isCompleted() = step.isCompleted()

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
//                    propertiesAfterCompletion = properties.copy()
                TODO()
                return true
            }
        }
        return false
    }
}
