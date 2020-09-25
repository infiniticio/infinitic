package io.infinitic.engine.pulsar.taskManager.functions

import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.common.taskManager.avro.AvroConverter
import io.infinitic.messaging.pulsar.PulsarTransport
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.engine.taskManager.storage.AvroKeyValueTaskStateStorage
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNamePulsarFunction : Function<AvroEnvelopeForMonitoringPerName, Void> {

    override fun process(input: AvroEnvelopeForMonitoringPerName, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        val message = AvroConverter.fromMonitoringPerName(input)

        try {
            getMonitoringPerName(ctx).handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        null
    }

    internal fun getMonitoringPerName(context: Context): MonitoringPerName {
        val storage = AvroKeyValueTaskStateStorage(PulsarFunctionStorage(context))
        val dispatcher = AvroDispatcher(PulsarTransport.forPulsarFunctionContext(context))

        return MonitoringPerName(storage, dispatcher)
    }
}
