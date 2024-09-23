package io.infinitic.common.transport.consumers

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ConsumerUniqueProcessor<S : TransportMessage, D : Any>(
  private val consumer: TransportConsumer<S>,
  deserialize: suspend (S) -> D,
  process: suspend (D, MillisInstant) -> Unit,
  beforeNegativeAcknowledgement: (suspend (S, D?, Exception) -> Unit)?
) : AbstractConsumerProcessor<S, D>(
    consumer,
    deserialize,
    process,
    beforeNegativeAcknowledgement,
) {

  suspend fun start() = consumer
      .receiveAsFlow()
      .collect { message ->
        withContext(NonCancellable) {
          tryDeserialize(message)?.let { process(it) }
        }
      }
}








