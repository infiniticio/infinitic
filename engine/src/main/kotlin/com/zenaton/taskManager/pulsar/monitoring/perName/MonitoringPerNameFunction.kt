package com.zenaton.taskManager.pulsar.monitoring.perName

import com.zenaton.jobManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameEngine
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.taskManager.pulsar.logger.PulsarLogger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNameFunction : Function<AvroMonitoringPerNameMessage, Void> {
    // task metrics injection
    var taskMetrics = MonitoringPerNameEngine()
    // avro converter injection
    var avroConverter = AvroConverter

    override fun process(input: AvroMonitoringPerNameMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskMetrics.logger = PulsarLogger(ctx)
        taskMetrics.storage = MonitoringPerNamePulsarStorage(ctx)
        taskMetrics.dispatcher = PulsarDispatcher(ctx)

        try {
            taskMetrics.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            taskMetrics.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
