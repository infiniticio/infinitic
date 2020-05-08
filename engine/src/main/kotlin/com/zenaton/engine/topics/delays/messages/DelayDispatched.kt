package com.zenaton.engine.topics.delays.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DelayId
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.delays.interfaces.DelayMessageInterface

data class DelayDispatched(
    override var delayId: DelayId,
    override var sentAt: DateTime? = DateTime(),
    val delayDateTime: DateTime,
    val workflowId: WorkflowId? = null,
    val taskId: TaskId? = null
) : DelayMessageInterface
