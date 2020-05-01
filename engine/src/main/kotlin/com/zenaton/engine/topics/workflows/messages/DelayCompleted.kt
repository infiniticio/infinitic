package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.workflows.WorkflowId

data class DelayCompleted(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val delayId: DelayId
) : WorkflowMessageInterface
