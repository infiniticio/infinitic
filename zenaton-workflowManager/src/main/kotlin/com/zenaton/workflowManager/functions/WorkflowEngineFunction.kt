package com.zenaton.workflowManager.functions

import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.dispatcher.Dispatcher
import com.zenaton.workflowManager.engine.WorkflowEngine
import com.zenaton.workflowManager.engine.WorkflowEngineStateStorage
import com.zenaton.workflowManager.interfaces.AvroDispatcher
import com.zenaton.workflowManager.interfaces.AvroStorage
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import org.slf4j.Logger

class WorkflowEngineFunction {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    var engine = WorkflowEngine()

    fun handle(input: AvroEnvelopeForWorkflowEngine) {
        engine.logger = logger
        engine.storage = WorkflowEngineStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromWorkflowEngine(input))
    }
}
