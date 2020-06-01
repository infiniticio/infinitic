package com.zenaton.taskManager.pulsar.monitoring.global

import com.zenaton.taskManager.admin.messages.AvroMonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalEngine
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.taskManager.pulsar.logger.PulsarLogger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringGlobalFunction : Function<AvroMonitoringGlobalMessage, Void> {
    // task metrics injection
    var taskAdmin = MonitoringGlobalEngine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroMonitoringGlobalMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskAdmin.logger = PulsarLogger(ctx)
        taskAdmin.storage = MonitoringGlobalPulsarStorage(ctx)
        taskAdmin.dispatcher = PulsarDispatcher(ctx)

        try {
            taskAdmin.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            taskAdmin.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
