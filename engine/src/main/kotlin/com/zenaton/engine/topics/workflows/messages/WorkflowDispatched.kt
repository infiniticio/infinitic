package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.WorkflowData
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.WorkflowName
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
