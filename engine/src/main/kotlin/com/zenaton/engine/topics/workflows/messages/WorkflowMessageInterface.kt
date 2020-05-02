package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.workflows.WorkflowId

interface WorkflowMessageInterface {
    val workflowId: WorkflowId
    var receivedAt: DateTime?
    fun getKey() = workflowId.id
}
