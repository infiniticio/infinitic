package com.zenaton.engine.topics.delays.state

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DelayId
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.interfaces.StateInterface

class DelayState(val delayId: DelayId) : StateInterface {
    var workflowId: WorkflowId? = null
    var taskId: TaskId? = null
    var delayDateTime: DateTime? = null
}
