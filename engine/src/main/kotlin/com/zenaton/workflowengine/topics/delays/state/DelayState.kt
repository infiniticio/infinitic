package com.zenaton.workflowengine.topics.delays.state

import com.zenaton.commons.data.DateTime
import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowengine.data.DelayId
import com.zenaton.workflowengine.data.WorkflowId

class DelayState(val delayId: DelayId) : StateInterface {
    var workflowId: WorkflowId? = null
    var jobId: JobId? = null
    var delayDateTime: DateTime? = null
}
