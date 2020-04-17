package com.zenaton.engine.workflows.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zenaton.engine.common.attributes.DelayId
import com.zenaton.engine.common.attributes.TaskId
import com.zenaton.engine.common.attributes.WorkflowId

sealed class WorkflowMessage(val type: String, open var workflowId: WorkflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DelayCompleted(
    override var workflowId: WorkflowId,
    val delayId: DelayId
) : WorkflowMessage("DelayCompleted", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    val workflowName: String,
    val workflowData: String
) : WorkflowMessage("WorkflowDispatched", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskCompleted(
    override var workflowId: WorkflowId,
    val taskId: TaskId,
    val taskData: String
) : WorkflowMessage("TaskCompleted", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionCompleted(
    override var workflowId: WorkflowId
) : WorkflowMessage("DecisionCompleted", workflowId)
