package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.storage.AvroKeyValueTaskStateStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringGlobalPulsarFunction : Function<AvroEnvelopeForMonitoringGlobal, Void> {

    override fun process(input: AvroEnvelopeForMonitoringGlobal, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        val message = AvroConverter.fromMonitoringGlobal(input)

        try {
            getMonitoringGlobal(ctx).handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }

    internal fun getMonitoringGlobal(ctx: Context): MonitoringGlobal {
        val storage = AvroKeyValueTaskStateStorage(PulsarFunctionStorage(ctx))

        return MonitoringGlobal(storage)
    }
}
