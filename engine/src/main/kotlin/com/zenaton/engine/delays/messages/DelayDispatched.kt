package com.zenaton.engine.delays.messages

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.workflows.data.WorkflowId

data class DelayDispatched(
    override var delayId: DelayId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val delayDateTime: DateTime,
    val workflowId: WorkflowId? = null,
    val taskId: TaskId? = null
) : DelayMessageInterface
