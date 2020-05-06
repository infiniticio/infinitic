package com.zenaton.engine.workflows.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowData
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.data.WorkflowName
import com.zenaton.engine.workflows.interfaces.WorkflowMessageInterface

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
