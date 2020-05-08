package com.zenaton.engine.topics.workflows.interfaces

import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.interfaces.MessageInterface

interface WorkflowMessageInterface : MessageInterface {
    val workflowId: WorkflowId
    override fun getKey() = workflowId.id
}
