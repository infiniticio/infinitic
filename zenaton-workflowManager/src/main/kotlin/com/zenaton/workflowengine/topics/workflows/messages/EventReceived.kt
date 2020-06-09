package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowengine.data.EventData
import com.zenaton.workflowengine.data.EventName
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class EventReceived(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val eventName: EventName,
    val eventData: EventData?
) : WorkflowMessageInterface
