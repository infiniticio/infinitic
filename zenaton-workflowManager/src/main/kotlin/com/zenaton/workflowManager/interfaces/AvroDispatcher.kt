package com.zenaton.workflowManager.interfaces

import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.workflowManager.messages.envelopes.AvroForWorkflowEngineMessage

interface AvroDispatcher {
    fun toWorkflowEngine(msg: AvroForWorkflowEngineMessage, after: Float = 0f)
    fun toDeciders(msg: AvroForJobEngineMessage)
    fun toWorkers(msg: AvroForJobEngineMessage)
}
