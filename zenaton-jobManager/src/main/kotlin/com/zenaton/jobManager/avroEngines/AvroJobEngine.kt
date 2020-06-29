package com.zenaton.jobManager.avroEngines

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.engines.JobEngine
import com.zenaton.jobManager.storages.JobEngineStateStorage
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import org.slf4j.Logger

class AvroJobEngine {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    var engine = JobEngine()

    fun handle(input: AvroEnvelopeForJobEngine) {
        engine.logger = logger
        engine.storage = JobEngineStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromJobEngine(input))
    }
}
