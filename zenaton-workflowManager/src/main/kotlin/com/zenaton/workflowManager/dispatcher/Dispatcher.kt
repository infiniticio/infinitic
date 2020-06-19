package com.zenaton.workflowManager.dispatcher

import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.interfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.ForDecisionEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForTaskEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float = 0f) {
        avroDispatcher.toWorkflowEngine(AvroConverter.toWorkflowEngine(msg))
    }

    fun toDeciders(msg: ForDecisionEngineMessage) {
        avroDispatcher.toDeciders(AvroConverter.toDecisionEngine(msg))
    }

    fun toWorkers(msg: ForTaskEngineMessage) {
        avroDispatcher.toWorkers(AvroConverter.toTaskEngine(msg))
    }
}
