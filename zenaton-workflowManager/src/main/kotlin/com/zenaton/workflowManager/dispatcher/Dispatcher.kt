package com.zenaton.workflowManager.dispatcher

import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.interfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.envelopes.ForDecidersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float = 0f) {
        avroDispatcher.toWorkflowEngine(AvroConverter.toWorkflowEngine(msg))
    }

    fun toDeciders(msg: ForDecidersMessage) {
        avroDispatcher.toDeciders(AvroConverter.toJobEngine(msg))
    }

    fun toWorkers(msg: ForWorkersMessage) {
        avroDispatcher.toWorkers(AvroConverter.toJobEngine(msg))
    }
}
