package com.zenaton.engine.data.delays

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.workflows.WorkflowId

class DelayState(val delayId: DelayId) {
    var workflowId: WorkflowId? = null
    var taskId: TaskId? = null
    var delayDateTime: DateTime? = null

    fun getKey() = delayId.id
}
