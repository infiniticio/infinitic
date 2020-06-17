package com.zenaton.jobManager.functions

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.engine.JobEngine
import com.zenaton.jobManager.engine.JobEngineStateStorage
import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import org.slf4j.Logger

class JobEngineFunction {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    var engine = JobEngine()

    fun handle(input: AvroForJobEngineMessage) {
        engine.logger = logger
        engine.storage = JobEngineStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromJobEngine(input))
    }
}
