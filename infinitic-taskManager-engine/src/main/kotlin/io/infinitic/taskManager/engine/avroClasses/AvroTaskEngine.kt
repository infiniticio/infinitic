package io.infinitic.taskManager.engine.avroClasses

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.engine.storages.TaskEngineStateStorage
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
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
