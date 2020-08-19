package io.infinitic.taskManager.engine.avroClasses

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.storages.MonitoringGlobalStorage
import org.slf4j.Logger

class AvroMonitoringGlobal {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage

    private val monitoring = MonitoringGlobal()

    fun handle(input: AvroEnvelopeForMonitoringGlobal) {
        monitoring.logger = logger
        monitoring.storage = MonitoringGlobalStorage(avroStorage)

        monitoring.handle(AvroConverter.fromMonitoringGlobal(input))
    }
}
