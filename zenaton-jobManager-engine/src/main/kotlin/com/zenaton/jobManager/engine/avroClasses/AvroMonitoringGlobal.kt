package com.zenaton.jobManager.engine.avroClasses

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.engine.engines.MonitoringGlobal
import com.zenaton.jobManager.engine.storages.MonitoringGlobalStorage
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
