package com.zenaton.workflowManager.topics.workflows.interfaces

import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.interfaces.MessageInterface

interface WorkflowMessageInterface : MessageInterface {
    val workflowId: WorkflowId
    override fun getStateId() = workflowId.id
}
