package com.zenaton.taskManager.engine.avroClasses

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.taskManager.engine.engines.MonitoringGlobal
import com.zenaton.taskManager.engine.storages.MonitoringGlobalStorage
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
