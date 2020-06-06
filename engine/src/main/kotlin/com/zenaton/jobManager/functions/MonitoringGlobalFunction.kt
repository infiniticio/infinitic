package com.zenaton.jobManager.functions

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.monitoringGlobal.MonitoringGlobal
import com.zenaton.jobManager.storage.Storage
import org.slf4j.Logger

class MonitoringGlobalFunction {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage

    val monitoring = MonitoringGlobal()

    fun handle(input: AvroForMonitoringGlobalMessage) {
        monitoring.logger = logger
        monitoring.storage = Storage(avroStorage)

        monitoring.handle(AvroConverter.fromMonitoringGlobal(input))
    }
}
