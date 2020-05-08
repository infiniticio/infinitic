package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.EventData
import com.zenaton.engine.data.EventName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface

data class EventReceived(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowMessageInterface
