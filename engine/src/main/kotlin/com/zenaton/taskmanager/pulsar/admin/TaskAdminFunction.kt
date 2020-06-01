package com.zenaton.taskmanager.pulsar.admin

import com.zenaton.taskmanager.metrics.TaskMetrics
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.dispatcher.PulsarTaskDispatcher
import com.zenaton.taskmanager.pulsar.logger.PulsarTaskLogger
import com.zenaton.taskmanager.pulsar.metrics.state.PulsarTaskMetricsStateStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskAdminFunction : Function<AvroTaskMetricMessage, Void> {
    // task metrics injection
    var taskMetrics = TaskMetrics()
    // avro converter injection
    var avroConverter = TaskAvroConverter

    override fun process(input: AvroTaskMetricMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskMetrics.logger = PulsarTaskLogger(ctx)
        taskMetrics.storage = PulsarTaskMetricsStateStorage(ctx)
        taskMetrics.dispatcher = PulsarTaskDispatcher(ctx)

        try {
            taskMetrics.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            taskMetrics.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
