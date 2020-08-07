package com.zenaton.jobManager.worker.avroClasses

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.worker.Dispatcher
import com.zenaton.jobManager.worker.Worker
import com.zenaton.jobManager.worker.avroInterfaces.AvroDispatcher

class AvroWorker {
    lateinit var avroDispatcher: AvroDispatcher
    private val worker = Worker()

    fun handle(msg: AvroEnvelopeForWorker) {
        worker.dispatcher = Dispatcher(avroDispatcher)

        worker.handle(AvroConverter.fromWorkers(msg))
    }
}
