package com.zenaton.jobManager.pulsar.monitoring.global

import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalEngine
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringGlobalFunction : Function<AvroForMonitoringGlobalMessage, Void> {
    // task metrics injection
    var monitoringGlobal = MonitoringGlobalEngine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroForMonitoringGlobalMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        monitoringGlobal.logger = ctx.logger
        monitoringGlobal.storage = MonitoringGlobalPulsarStorage(ctx)
        monitoringGlobal.dispatcher = PulsarDispatcher(ctx)

        try {
            monitoringGlobal.handle(avroConverter.fromAvroForMonitoringGlobalMessage(input))
        } catch (e: Exception) {
            monitoringGlobal.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
