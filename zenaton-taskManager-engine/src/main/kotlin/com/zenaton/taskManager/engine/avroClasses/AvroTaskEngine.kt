package com.zenaton.taskManager.engine.avroClasses

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.engine.engines.TaskEngine
import com.zenaton.taskManager.engine.storages.TaskEngineStateStorage
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import org.slf4j.Logger

class AvroTaskEngine {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    private val engine = TaskEngine()

    fun handle(input: AvroEnvelopeForTaskEngine) {
        engine.logger = logger
        engine.storage = TaskEngineStateStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromTaskEngine(input))
    }
}
