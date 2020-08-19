package io.infinitic.workflowManager.engine.dispatcher

import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.engine.avroConverter.AvroConverter
import io.infinitic.workflowManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.workflowManager.engine.messages.ForWorkflowEngineMessage
import io.infinitic.taskManager.common.avro.AvroConverter as AvroTaskConverter

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float = 0f) {
        avroDispatcher.toWorkflowEngine(AvroConverter.toWorkflowEngine(msg))
    }

    fun toDeciders(msg: DispatchTask) {
        avroDispatcher.toDeciders(AvroTaskConverter.toTaskEngine(msg))
    }

    fun toWorkers(msg: DispatchTask) {
        avroDispatcher.toWorkers(AvroTaskConverter.toTaskEngine(msg))
    }
}
