package com.zenaton.workflowManager.data.state

import com.zenaton.commons.data.interfaces.IdInterface
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventId
import com.zenaton.workflowManager.data.WorkflowId
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : IdInterface {
    constructor(jobId: JobId) : this(jobId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
