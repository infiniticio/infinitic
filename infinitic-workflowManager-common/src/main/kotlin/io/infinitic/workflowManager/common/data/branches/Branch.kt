package io.infinitic.workflowManager.common.data.branches

import io.infinitic.common.data.DateTime
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.common.data.DelayId
import io.infinitic.workflowManager.common.data.events.EventData
import io.infinitic.workflowManager.common.data.events.EventName
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.commands.Command
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.commands.DispatchChildWorkflow
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.commands.WaitDelay
import io.infinitic.workflowManager.common.data.commands.WaitEvent
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.steps.BlockingStep
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodOutput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethod
import io.infinitic.workflowManager.common.data.workflows.WorkflowMethodInput

data class Branch(
    val branchId: BranchId,
    val workflowMethod: WorkflowMethod,
    val workflowMethodInput: WorkflowMethodInput,
    val propertiesAtStart: Properties,
    val dispatchedAt: DateTime = DateTime(),
    val blockingSteps: List<BlockingStep>,
    val commands: List<Command> = listOf()
) {
    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = commands
            .filterIsInstance<DispatchTask>()
            .firstOrNull { a -> a.taskId == taskId && a.actionStatus != CommandStatus.COMPLETED }
        task?.taskOutput = taskOutput
        task?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return blockingSteps.last().completeTask(taskId, properties)
    }

    fun completeChildWorkflow(childWorkflowId: WorkflowId, childWorkflowOutput: WorkflowMethodOutput, properties: Properties): Boolean {
        // complete action if relevant
        val childWorkflow = commands
            .filterIsInstance<DispatchChildWorkflow>()
            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.actionStatus != CommandStatus.COMPLETED }
        childWorkflow?.childWorkflowOutput = childWorkflowOutput
        childWorkflow?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return blockingSteps.last().completeChildWorkflow(childWorkflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = commands
            .filterIsInstance<WaitDelay>()
            .firstOrNull { a -> a.delayId == delayId && a.actionStatus != CommandStatus.COMPLETED }
        delay?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return blockingSteps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData, properties: Properties): Boolean {
        // complete action if relevant
        val event = commands
            .filterIsInstance<WaitEvent>()
            .firstOrNull { a -> a.eventName == eventName && a.actionStatus != CommandStatus.COMPLETED }
        event ?.eventData = eventData
        event ?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return if (event != null) blockingSteps.last().completeEvent(event.eventId, properties) else false
    }
}
