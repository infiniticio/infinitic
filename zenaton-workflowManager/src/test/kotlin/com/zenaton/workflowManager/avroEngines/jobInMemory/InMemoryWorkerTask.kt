package com.zenaton.workflowManager.avroEngines.jobInMemory

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorker.Status

internal class InMemoryWorkerTask : InMemoryWorker {
    override lateinit var avroDispatcher: AvroDispatcher
    lateinit var behavior: (msg: AvroRunJob) -> Status
    lateinit var taskA: Task
    lateinit var taskB: Task
    lateinit var taskC: Task

    override fun handle(msg: AvroEnvelopeForWorker) {
        when (val avro = AvroConverter.removeEnvelopeFromWorkerMessage(msg)) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                val out = when (avro.jobName) {
                    "TaskA" -> taskA.handle()
                    "TaskB" -> taskB.handle()
                    "TaskC" -> taskC.handle()
                    else -> throw Exception("Unknown job ${avro.jobName}")
                }
                when (behavior(avro)) {
                    Status.COMPLETED -> sendJobCompleted(avro, out)
                    Status.FAILED_WITH_RETRY -> sendJobFailed(avro, Exception("Will Try Again"), 0.1F)
                    Status.FAILED_WITHOUT_RETRY -> sendJobFailed(avro, Exception("Failed"))
                }
            }
        }
    }
}
