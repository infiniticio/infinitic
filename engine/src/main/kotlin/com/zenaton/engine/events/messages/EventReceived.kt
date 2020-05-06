package com.zenaton.engine.events.messages

import com.zenaton.engine.events.data.EventData
import com.zenaton.engine.events.data.EventName
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.interfaces.WorkflowMessageInterface

data class EventReceived(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowMessageInterface
