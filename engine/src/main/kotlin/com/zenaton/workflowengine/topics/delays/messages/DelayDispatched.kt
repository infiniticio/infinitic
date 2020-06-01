package com.zenaton.workflowengine.topics.delays.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskId
import com.zenaton.workflowengine.data.DelayId
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.topics.delays.interfaces.DelayMessageInterface

data class DelayDispatched(
    override var delayId: DelayId,
    override var sentAt: DateTime? = DateTime(),
    val delayDateTime: DateTime,
    val workflowId: WorkflowId? = null,
    val taskId: TaskId? = null
) : DelayMessageInterface
