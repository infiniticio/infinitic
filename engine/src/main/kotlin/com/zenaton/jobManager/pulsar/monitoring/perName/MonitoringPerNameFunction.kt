package com.zenaton.jobManager.pulsar.monitoring.perName

import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameEngine
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNameFunction : Function<AvroForMonitoringPerNameMessage, Void> {
    // task metrics injection
    var monitoringPerName = MonitoringPerNameEngine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroForMonitoringPerNameMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        monitoringPerName.logger = ctx.logger
        monitoringPerName.storage = MonitoringPerNamePulsarStorage(ctx)
        monitoringPerName.dispatcher = PulsarDispatcher(ctx)

        try {
            monitoringPerName.handle(avroConverter.fromAvroForMonitoringPerNameMessage(input))
        } catch (e: Exception) {
            monitoringPerName.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
