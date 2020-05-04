package com.zenaton.engine.workflows.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.messages.MessageInterface
import com.zenaton.engine.workflows.data.WorkflowId

interface WorkflowMessageInterface : MessageInterface {
    val workflowId: WorkflowId
    override var receivedAt: DateTime?
    override fun getKey() = workflowId.id
}
