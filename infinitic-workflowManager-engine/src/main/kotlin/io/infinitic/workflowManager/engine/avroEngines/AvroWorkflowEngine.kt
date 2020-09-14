package io.infinitic.workflowManager.engine.avroEngines

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.workflowManager.engine.storages.WorkflowStateStorage
import io.infinitic.workflowManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.workflowManager.engine.avroInterfaces.AvroStorage
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import org.slf4j.Logger

class AvroWorkflowEngine {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    var engine = WorkflowEngine()

    suspend fun handle(input: AvroEnvelopeForWorkflowEngine) {
        engine.logger = logger
        engine.storage = WorkflowStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromWorkflowEngine(input))
    }
}
