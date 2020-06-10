package com.zenaton.workflowManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.WorkflowData
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.WorkflowName
import com.zenaton.workflowManager.topics.workflows.interfaces.WorkflowMessageInterface

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
