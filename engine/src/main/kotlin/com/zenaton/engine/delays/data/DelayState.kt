package com.zenaton.engine.delays.data

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.data.StateInterface
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.workflows.data.WorkflowId

class DelayState(val delayId: DelayId) : StateInterface {
    var workflowId: WorkflowId? = null
    var taskId: TaskId? = null
    var delayDateTime: DateTime? = null
}
