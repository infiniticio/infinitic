package com.zenaton.workflowManager.topics.delays.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.topics.delays.interfaces.DelayMessageInterface

data class DelayDispatched(
    override var delayId: DelayId,
    override var sentAt: DateTime? = DateTime(),
    val delayDateTime: DateTime,
    val workflowId: WorkflowId? = null,
    val jobId: JobId? = null
) : DelayMessageInterface
