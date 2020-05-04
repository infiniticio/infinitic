package com.zenaton.engine.workflows.messages

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId

data class DelayCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val delayId: DelayId
) : WorkflowMessageInterface
