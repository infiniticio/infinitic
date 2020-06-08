package com.zenaton.workflowengine.topics.workflows.interfaces

import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.interfaces.MessageInterface

interface WorkflowMessageInterface : MessageInterface {
    val workflowId: WorkflowId
    override fun getStateId() = workflowId.id
}
