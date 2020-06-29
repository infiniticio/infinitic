package com.zenaton.workflowManager.avroEngines

import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.dispatcher.Dispatcher
import com.zenaton.workflowManager.engines.WorkflowEngine
import com.zenaton.workflowManager.storages.WorkflowEngineStateStorage
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.avroInterfaces.AvroStorage
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import org.slf4j.Logger

class AvroWorkflowEngine {
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
