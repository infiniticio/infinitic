package com.zenaton.workflowManager.dispatcher

import com.zenaton.taskManager.common.messages.DispatchTask
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage
import com.zenaton.taskManager.common.avro.AvroConverter as AvroTaskConverter

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
