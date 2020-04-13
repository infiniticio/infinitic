package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.common.attributes.DelayId
import com.zenaton.engine.common.attributes.TaskId
import com.zenaton.engine.common.attributes.WorkflowId

sealed class Message() {
    abstract val workflowId: WorkflowId

    data class WorkflowDispatched(
        override val workflowId: WorkflowId,
        val workflowName: String,
        val workflowData: String
    ) : Message()

    data class WorkflowCompleted(
        override val workflowId: WorkflowId
    ) : Message()

    data class DecisionCompleted(
        override val workflowId: WorkflowId,
        val decisionId: DecisionId
    ) : Message()

    data class TaskCompleted(
        override val workflowId: WorkflowId,
        val taskId: TaskId
    ) : Message()

    data class DelayCompleted(
        override val workflowId: WorkflowId,
        val delayId: DelayId
    ) : Message()
}

