package com.zenaton.jobManager.engine.avroClasses

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.dispatcher.Dispatcher
import com.zenaton.jobManager.engine.engines.JobEngine
import com.zenaton.jobManager.engine.storages.JobEngineStateStorage
import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.engine.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import org.slf4j.Logger

class AvroJobEngine {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    private val engine = JobEngine()

    fun handle(input: AvroEnvelopeForJobEngine) {
        engine.logger = logger
        engine.storage = JobEngineStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromJobEngine(input))
    }
}
