package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.functions.MonitoringPerNameFunction
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.jobManager.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNamePulsarFunction : Function<AvroForMonitoringPerNameMessage, Void> {

    var monitoring = MonitoringPerNameFunction()

    override fun process(input: AvroForMonitoringPerNameMessage, context: Context?): Void? {
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
