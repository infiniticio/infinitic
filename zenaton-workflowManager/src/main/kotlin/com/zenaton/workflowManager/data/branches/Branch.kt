package com.zenaton.workflowManager.data.branches

import com.zenaton.common.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobOutput
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventData
import com.zenaton.workflowManager.data.EventName
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.data.actions.ActionStatus
import com.zenaton.workflowManager.data.actions.DispatchChildWorkflow
import com.zenaton.workflowManager.data.actions.DispatchTask
import com.zenaton.workflowManager.data.actions.WaitDelay
import com.zenaton.workflowManager.data.actions.WaitEvent
import com.zenaton.workflowManager.data.properties.Properties
import com.zenaton.workflowManager.data.steps.Step

data class Branch(
    val branchId: BranchId = BranchId(),
    val branchName: BranchName,
    val branchInput: BranchInput,
    val propertiesAtStart: Properties = Properties(mapOf()),
    val dispatchedAt: DateTime = DateTime(),
    val steps: List<Step> = listOf(),
    val actions: List<Action> = listOf()
) {
    fun completeTask(taskId: JobId, taskOutput: JobOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = actions
            .filterIsInstance<DispatchTask>()
            .firstOrNull { a -> a.taskId == taskId && a.actionStatus != ActionStatus.COMPLETED }
        task?.taskOutput = taskOutput
        task?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeTask(taskId, properties)
    }

    fun completeChildWorkflow(childWorkflowId: WorkflowId, childWorkflowOutput: BranchOutput, properties: Properties): Boolean {
        // complete action if relevant
        val childWorkflow = actions
            .filterIsInstance<DispatchChildWorkflow>()
            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.actionStatus != ActionStatus.COMPLETED }
        childWorkflow?.childWorkflowOutput = childWorkflowOutput
        childWorkflow?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeChildWorkflow(childWorkflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = actions
            .filterIsInstance<WaitDelay>()
            .firstOrNull { a -> a.delayId == delayId && a.actionStatus != ActionStatus.COMPLETED }
        delay?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData, properties: Properties): Boolean {
        // complete action if relevant
        val event = actions
            .filterIsInstance<WaitEvent>()
            .firstOrNull { a -> a.eventName == eventName && a.actionStatus != ActionStatus.COMPLETED }
        event ?.eventData = eventData
        event ?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return if (event != null) steps.last().completeEvent(event.eventId, properties) else false
    }
}
