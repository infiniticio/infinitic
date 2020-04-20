package com.zenaton.engine.workflows

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zenaton.engine.common.attributes.BranchData
import com.zenaton.engine.common.attributes.DateTime
import com.zenaton.engine.common.attributes.DelayId
import com.zenaton.engine.common.attributes.TaskData
import com.zenaton.engine.common.attributes.TaskId
import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.common.attributes.WorkflowName

sealed class WorkflowMessage(val type: String, open var workflowId: WorkflowId) {
    fun getStateKey() = workflowId.id
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DelayCompleted(
    override var workflowId: WorkflowId,
    val delayId: DelayId
) : WorkflowMessage("DelayCompleted", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowData: BranchData?,
    val dispatchedAt: DateTime
) : WorkflowMessage("WorkflowDispatched", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskCompleted(
    override var workflowId: WorkflowId,
    val taskId: TaskId,
    val taskData: TaskData?
) : WorkflowMessage("TaskCompleted", workflowId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionCompleted(
    override var workflowId: WorkflowId
) : WorkflowMessage("DecisionCompleted", workflowId)
