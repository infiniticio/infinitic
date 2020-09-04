package io.infinitic.workflowManager.common.data.branches

import io.infinitic.common.data.DateTime
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.events.EventData
import io.infinitic.workflowManager.common.data.events.EventName
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.commands.ChildWorkflowDispatched
import io.infinitic.workflowManager.common.data.commands.DelayWaited
import io.infinitic.workflowManager.common.data.commands.EventWaited
import io.infinitic.workflowManager.common.data.commands.TaskDispatched
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.PastStep
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodOutput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethod
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodInput

data class Branch(
    val branchId: BranchId,
    val workflowMethod: WorkflowMethod,
    val workflowMethodInput: WorkflowMethodInput,
    val propertiesAtStart: Properties,
    val dispatchedAt: DateTime = DateTime(),
    val pastSteps: List<PastStep>,
    val pastCommands: List<PastCommand> = listOf()
) {
    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = pastCommands
            .filterIsInstance<TaskDispatched>()
            .firstOrNull { a -> a.taskId == taskId && a.commandStatus != CommandStatus.COMPLETED }
        task?.taskOutput = taskOutput
        task?.commandStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return pastSteps.last().completeTask(taskId, properties)
    }

    fun completeChildWorkflow(childWorkflowId: WorkflowId, childWorkflowOutput: WorkflowMethodOutput, properties: Properties): Boolean {
        // complete action if relevant
        val childWorkflow = pastCommands
            .filterIsInstance<ChildWorkflowDispatched>()
            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.commandStatus != CommandStatus.COMPLETED }
        childWorkflow?.childWorkflowOutput = childWorkflowOutput
        childWorkflow?.commandStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return pastSteps.last().completeChildWorkflow(childWorkflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = pastCommands
            .filterIsInstance<DelayWaited>()
            .firstOrNull { a -> a.delayId == delayId && a.commandStatus != CommandStatus.COMPLETED }
        delay?.commandStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return pastSteps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData, properties: Properties): Boolean {
        // complete action if relevant
        val event = pastCommands
            .filterIsInstance<EventWaited>()
            .firstOrNull { a -> a.eventName == eventName && a.commandStatus != CommandStatus.COMPLETED }
        event ?.eventData = eventData
        event ?.commandStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return if (event != null) pastSteps.last().completeEvent(event.eventId, properties) else false
    }
}
