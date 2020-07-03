package com.zenaton.workflowManager.avroEngines.jobInMemory

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorker.Status

internal class InMemoryWorkerDecision : InMemoryWorker {
    override lateinit var avroDispatcher: AvroDispatcher
    lateinit var behavior: (msg: AvroRunJob) -> Status
    lateinit var workflowA: Workflow

    override fun handle(msg: AvroEnvelopeForWorker) {
        when (val avro = AvroConverter.removeEnvelopeFromWorkerMessage(msg)) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                val out = when (avro.jobName) {
                    "WorkflowA" -> workflowA.handle()
                    else -> throw Exception("Unknown job ${avro.jobName}")
                }
                sendJobCompleted(avro, out)
            }
        }
    }
}
