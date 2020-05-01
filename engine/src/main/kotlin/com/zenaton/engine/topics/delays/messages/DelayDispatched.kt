package com.zenaton.engine.topics.delays.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.workflows.WorkflowId

data class DelayDispatched(
    override var delayId: DelayId,
    override var receivedAt: DateTime? = null,
    val delayDateTime: DateTime,
    val workflowId: WorkflowId? = null,
    val taskId: TaskId? = null
) : DelayMessageInterface
