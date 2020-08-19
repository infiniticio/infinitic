package com.zenaton.taskManager.engine.avroClasses

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.engine.engines.JobEngine
import com.zenaton.taskManager.engine.storages.JobEngineStateStorage
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine
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
