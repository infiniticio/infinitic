package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowengine.data.WorkflowData
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.data.WorkflowName
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class WorkflowDispatched(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val workflowName: WorkflowName,
    val workflowData: WorkflowData?,
    val parentWorkflowId: WorkflowId? = null,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
