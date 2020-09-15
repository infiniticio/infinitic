package io.infinitic.workflowManager.client

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage

class WorkflowDispatcher(private val avroDispatcher: AvroWorkflowDispatcher) {
    suspend fun toWorkflowEngine(msg: ForWorkflowEngineMessage) {
        avroDispatcher.toWorkflowEngine(AvroConverter.toWorkflowEngine(msg))
    }
}
