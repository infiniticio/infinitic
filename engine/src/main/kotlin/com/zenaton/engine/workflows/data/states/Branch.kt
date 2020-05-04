package com.zenaton.engine.workflows.data.states

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.events.data.EventData
import com.zenaton.engine.events.data.EventName
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskOutput
import com.zenaton.engine.workflows.data.WorkflowData
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.data.WorkflowOutput

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    Type(value = Branch.Handle::class, name = "HANDLE"),
    Type(value = Branch.OnEvent::class, name = "ON_EVENT")
)
sealed class Branch(
    open val branchId: BranchId,
    open val dispatchedAt: DateTime,
    open val propertiesAtStart: Properties = Properties(),
    open val steps: List<Step> = listOf<Step>(),
    open val actions: List<Action> = listOf<Action>()
) {
    data class Handle(
        override val branchId: BranchId = BranchId(),
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: List<Action> = listOf<Action>(),
        val workflowData: WorkflowData?
    ) : Branch(branchId, dispatchedAt, propertiesAtStart, steps, actions)

    data class OnEvent(
        override val branchId: BranchId = BranchId(),
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: List<Action> = listOf<Action>(),
        val eventName: EventName,
        val eventData: EventData?
    ) : Branch(branchId, dispatchedAt, propertiesAtStart, steps, actions)

    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = actions
            .filterIsInstance<DispatchTask>()
            .firstOrNull { a -> a.taskId == taskId && a.actionStatus != ActionStatus.COMPLETED }
        task ?.taskOutput = taskOutput
        task ?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeTask(taskId, properties)
    }

    fun completeChildWorkflow(childWorkflowId: WorkflowId, childWorkflowOutput: WorkflowOutput, properties: Properties): Boolean {
        // complete action if relevant
        val childWorkflow = actions
            .filterIsInstance<DispatchChildWorkflow>()
            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.actionStatus != ActionStatus.COMPLETED }
        childWorkflow ?.childWorkflowOutput = childWorkflowOutput
        childWorkflow ?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeChildWorkflow(childWorkflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = actions
            .filterIsInstance<WaitDelay>()
            .firstOrNull { a -> a.delayId == delayId && a.actionStatus != ActionStatus.COMPLETED }
        delay ?.actionStatus = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData?, properties: Properties): Boolean {
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
