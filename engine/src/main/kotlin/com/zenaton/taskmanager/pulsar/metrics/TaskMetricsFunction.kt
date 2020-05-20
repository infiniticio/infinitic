package com.zenaton.taskmanager.pulsar.metrics

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.taskmanager.messages.events.AvroTaskStatusUpdated
import com.zenaton.taskmanager.metrics.TaskMetrics
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.state.StateStorageImpl
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskMetricsFunction : Function<AvroTaskStatusUpdated, Void> {
    override fun process(input: AvroTaskStatusUpdated, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from tasks.StateFunction")

        try {
            TaskMetrics(StateStorageImpl(context)).handle(TaskAvroConverter.fromAvro(input))
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
