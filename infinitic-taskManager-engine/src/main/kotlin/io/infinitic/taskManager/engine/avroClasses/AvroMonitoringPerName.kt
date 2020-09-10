package io.infinitic.taskManager.engine.avroClasses

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.storages.MonitoringPerNameStorage
import org.slf4j.Logger

class AvroMonitoringPerName {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    private val monitoring = MonitoringPerName()

    suspend fun handle(input: AvroEnvelopeForMonitoringPerName) {
        monitoring.logger = logger
        monitoring.storage = MonitoringPerNameStorage(avroStorage)
        monitoring.dispatcher = Dispatcher(avroDispatcher)

        monitoring.handle(AvroConverter.fromMonitoringPerName(input))
    }
}
