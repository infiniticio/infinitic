package com.zenaton.jobManager.worker.avro

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.worker.Dispatcher
import com.zenaton.jobManager.worker.Worker

class AvroWorker {
    lateinit var avroDispatcher: AvroDispatcher
    private val worker = Worker()

    fun handle(msg: AvroEnvelopeForWorker) {
        worker.dispatcher = Dispatcher(avroDispatcher)

        worker.handle(AvroConverter.fromWorkers(msg))
    }
}
