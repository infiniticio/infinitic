package com.zenaton.taskmanager.pulsar.metrics

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.taskmanager.messages.metrics.AvroTaskMetricMessage
import com.zenaton.taskmanager.metrics.TaskMetrics
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.state.PulsarTaskEngineStateStorage
import com.zenaton.taskmanager.pulsar.state.PulsarTaskMetricsStateStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskMetricsFunction : Function<AvroTaskMetricMessage, Void> {
    var taskMetrics = TaskMetrics()

    override fun process(input: AvroTaskMetricMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received in TaskMetricsFunction::process method.")

        try {
            taskMetrics.stateStorage = PulsarTaskMetricsStateStorage(context)
            taskMetrics.handle(TaskAvroConverter.fromAvro(input))
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
