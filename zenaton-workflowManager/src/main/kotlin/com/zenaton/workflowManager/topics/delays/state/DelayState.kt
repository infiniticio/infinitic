package com.zenaton.workflowManager.topics.delays.state

import com.zenaton.commons.data.DateTime
import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.WorkflowId

class DelayState(val delayId: DelayId) : StateInterface {
    var workflowId: WorkflowId? = null
    var jobId: JobId? = null
    var delayDateTime: DateTime? = null
}
