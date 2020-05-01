package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.workflows.WorkflowData
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowName

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
