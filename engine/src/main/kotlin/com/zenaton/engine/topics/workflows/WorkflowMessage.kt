package com.zenaton.engine.topics.workflows

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.events.EventData
import com.zenaton.engine.data.events.EventName
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.types.DateTime
import com.zenaton.engine.data.workflows.WorkflowData
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowName
import com.zenaton.engine.data.workflows.WorkflowOutput

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = WorkflowDispatched::class, name = "WORKFLOW_DISPATCHED"),
    JsonSubTypes.Type(value = WorkflowCompleted::class, name = "WORKFLOW_COMPLETED"),
    JsonSubTypes.Type(value = TaskCompleted::class, name = "TASK_COMPLETED"),
    JsonSubTypes.Type(value = DelayCompleted::class, name = "DELAY_COMPLETED"),
    JsonSubTypes.Type(value = DecisionCompleted::class, name = "DECISION_COMPLETED")
)
sealed class WorkflowMessage(open val workflowId: WorkflowId, open var receivedAt: DateTime? = null) {
    @JsonIgnore
    fun getStateKey() = workflowId.id
}

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessage(workflowId)

data class DecisionCompleted(
    override var workflowId: WorkflowId,
    val decisionId: DecisionId
) : WorkflowMessage(workflowId)

data class TaskCompleted(
    override var workflowId: WorkflowId,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessage(workflowId)

data class ChildWorkflowCompleted(
    override var workflowId: WorkflowId,
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: WorkflowOutput?
) : WorkflowMessage(workflowId)

data class DelayCompleted(
    override var workflowId: WorkflowId,
    val delayId: DelayId
) : WorkflowMessage(workflowId)

data class EventReceived(
    override var workflowId: WorkflowId,
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowMessage(workflowId)

data class WorkflowCompleted(
    override var workflowId: WorkflowId,
    val workflowOutput: WorkflowOutput?,
    val dispatchedAt: DateTime
) : WorkflowMessage(workflowId)
