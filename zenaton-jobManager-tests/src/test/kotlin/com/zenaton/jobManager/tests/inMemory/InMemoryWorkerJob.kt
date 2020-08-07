package com.zenaton.jobManager.tests.inMemory

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.tests.inMemory.InMemoryWorker.Status

internal class InMemoryWorkerJob : InMemoryWorker {
    override lateinit var avroDispatcher: AvroDispatcher
    lateinit var behavior: (msg: AvroRunJob) -> Status
    lateinit var jobA: Job

    override fun handle(msg: AvroEnvelopeForWorker) {

        when (val avro = AvroConverter.removeEnvelopeFromWorkerMessage(msg)) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                val input = avro.jobInput.map { AvroConverter.fromAvroSerializedData(it) }
                val out = when (avro.jobName) {
                    "JobA" -> jobA.handle()
                    else -> throw Exception("Unknown job ${avro.jobName}")
                }
                when (behavior(avro)) {
                    Status.COMPLETED -> sendJobCompleted(avro, out)
                    Status.FAILED_WITH_RETRY -> sendJobFailed(avro, Exception("Will Try Again"), 0.1F)
                    Status.FAILED_WITHOUT_RETRY -> sendJobFailed(avro, Exception("Job Failed"))
                }
            }
        }
    }
}
