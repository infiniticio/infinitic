package com.zenaton.taskManager.pulsar.functions

import com.zenaton.taskManager.engine.avroClasses.AvroMonitoringPerName
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.taskManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNamePulsarFunction : Function<AvroEnvelopeForMonitoringPerName, Void> {

    var monitoring = AvroMonitoringPerName()

    override fun process(input: AvroEnvelopeForMonitoringPerName, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            monitoring.logger = ctx.logger
            monitoring.avroStorage = PulsarAvroStorage(ctx)
            monitoring.avroDispatcher = PulsarAvroDispatcher(ctx)

            monitoring.handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
