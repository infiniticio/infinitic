package com.zenaton.engine.workflows

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.attributes.delays.DelayId
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.tasks.TaskOutput
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowData
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowName
import com.zenaton.engine.attributes.workflows.WorkflowOutput

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = WorkflowDispatched::class, name = "WORKFLOW_DISPATCHED"),
    JsonSubTypes.Type(value = WorkflowCompleted::class, name = "WORKFLOW_COMPLETED"),
    JsonSubTypes.Type(value = TaskCompleted::class, name = "TASK_COMPLETED"),
    JsonSubTypes.Type(value = DelayCompleted::class, name = "DELAY_COMPLETED"),
    JsonSubTypes.Type(value = DecisionCompleted::class, name = "DECISION_COMPLETED")
)
sealed class WorkflowMessage(open var workflowId: WorkflowId) {
    @JsonIgnore
    fun getStateKey() = workflowId.id
}

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val dispatchedAt: DateTime
) : WorkflowMessage(workflowId)

data class WorkflowCompleted(
    override var workflowId: WorkflowId,
    val workflowOutput: WorkflowOutput?,
    val dispatchedAt: DateTime
) : WorkflowMessage(workflowId)

data class TaskCompleted(
    override var workflowId: WorkflowId,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessage(workflowId)

data class DelayCompleted(
    override var workflowId: WorkflowId,
    val delayId: DelayId
) : WorkflowMessage(workflowId)

data class DecisionCompleted(
    override var workflowId: WorkflowId
) : WorkflowMessage(workflowId)
