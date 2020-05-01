package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.events.EventData
import com.zenaton.engine.data.events.EventName
import com.zenaton.engine.data.workflows.WorkflowId

data class EventReceived(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowMessageInterface
