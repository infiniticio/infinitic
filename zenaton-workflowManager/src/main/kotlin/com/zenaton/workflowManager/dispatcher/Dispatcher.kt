package com.zenaton.workflowManager.dispatcher

import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroInterfaces.AvroDispatcher
import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage
import com.zenaton.jobManager.avroConverter.AvroConverter as AvroJobConverter

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float = 0f) {
        avroDispatcher.toWorkflowEngine(AvroConverter.toWorkflowEngine(msg))
    }

    fun toDeciders(msg: DispatchJob) {
        avroDispatcher.toDeciders(AvroJobConverter.toJobEngine(msg))
    }

    fun toWorkers(msg: DispatchJob) {
        avroDispatcher.toWorkers(AvroJobConverter.toJobEngine(msg))
    }
}
