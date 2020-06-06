package com.zenaton.jobManager.functions

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.monitoringPerName.MonitoringPerName
import com.zenaton.jobManager.monitoringPerName.MonitoringPerNameStorage
import org.slf4j.Logger

class MonitoringPerNameFunction {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    val monitoring = MonitoringPerName()

    fun handle(input: AvroForMonitoringPerNameMessage) {
        monitoring.logger = logger
        monitoring.storage = MonitoringPerNameStorage(avroStorage)
        monitoring.dispatcher = Dispatcher(avroDispatcher)

        monitoring.handle(AvroConverter.fromMonitoringPerName(input))
    }
}
