package com.zenaton.jobManager.avroEngines

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.engines.MonitoringGlobal
import com.zenaton.jobManager.storages.MonitoringGlobalStorage
import org.slf4j.Logger

class AvroMonitoringGlobal {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage

    val monitoring = MonitoringGlobal()

    fun handle(input: AvroEnvelopeForMonitoringGlobal) {
        monitoring.logger = logger
        monitoring.storage = MonitoringGlobalStorage(avroStorage)

        monitoring.handle(AvroConverter.fromMonitoringGlobal(input))
    }
}
