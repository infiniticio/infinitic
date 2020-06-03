package com.zenaton.jobManager.pulsar.monitoring.global

import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalEngine
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.jobManager.pulsar.logger.PulsarLogger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringGlobalFunction : Function<AvroForMonitoringGlobalMessage, Void> {
    // task metrics injection
    var taskAdmin = MonitoringGlobalEngine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroForMonitoringGlobalMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskAdmin.logger = PulsarLogger(ctx)
        taskAdmin.storage = MonitoringGlobalPulsarStorage(ctx)
        taskAdmin.dispatcher = PulsarDispatcher(ctx)

        try {
            taskAdmin.handle(avroConverter.fromAvroForMonitoringGlobalMessage(input))
        } catch (e: Exception) {
            taskAdmin.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
