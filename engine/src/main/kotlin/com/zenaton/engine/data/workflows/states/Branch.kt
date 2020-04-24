package com.zenaton.engine.data.workflows.states

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventData
import com.zenaton.engine.data.events.EventName
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.types.DateTime
import com.zenaton.engine.data.workflows.WorkflowData
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowOutput

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    Type(value = Branch.Handle::class, name = "HANDLE"),
    Type(value = Branch.OnEvent::class, name = "ON_EVENT")
)
sealed class Branch(
    open val dispatchedAt: DateTime,
    open val propertiesAtStart: Properties = Properties(),
    open val steps: List<Step> = listOf<Step>(),
    open val actions: Set<Action> = setOf<Action>()
) {
    data class Handle(
        val workflowData: WorkflowData?,
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: Set<Action> = setOf<Action>()
    ) : Branch(dispatchedAt = dispatchedAt, propertiesAtStart = propertiesAtStart, steps = steps, actions = actions)

    data class OnEvent(
        val eventName: EventName,
        val eventData: EventData?,
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: Set<Action> = setOf<Action>()
    ) : Branch(dispatchedAt = dispatchedAt, propertiesAtStart = propertiesAtStart, steps = steps, actions = actions)

    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = actions
            .filterIsInstance<DispatchTask>()
            .firstOrNull { a -> a.taskId == taskId && a.status != ActionStatus.COMPLETED }
        task ?.taskOutput = taskOutput
        task ?.status = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeTask(taskId, properties)
    }

    fun completeWorkflow(workflowId: WorkflowId, workflowOutput: WorkflowOutput, properties: Properties): Boolean {
        // complete action if relevant
        val workflow = actions
            .filterIsInstance<DispatchWorkflow>()
            .firstOrNull { a -> a.workflowId == workflowId && a.status != ActionStatus.COMPLETED }
        workflow ?.workflowOutput = workflowOutput
        workflow ?.status = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeWorkflow(workflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = actions
            .filterIsInstance<DispatchDelay>()
            .firstOrNull { a -> a.delayId == delayId && a.status != ActionStatus.COMPLETED }
        delay ?.status = ActionStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData?, properties: Properties): Boolean {
        // complete action if relevant
        val event = actions
            .filterIsInstance<ListenEvent>()
            .firstOrNull { a -> a.eventName == eventName && a.status != ActionStatus.COMPLETED }
        event ?.eventData = eventData
        event ?.status = ActionStatus.COMPLETED
        // does this task complete the current step?
        return if (event != null) steps.last().completeEvent(event.eventId, properties) else false
    }
}
