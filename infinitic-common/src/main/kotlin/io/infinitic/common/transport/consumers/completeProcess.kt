/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.transport.consumers

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M : Any> Channel<Result<T, T>>.completeProcess(
  concurrency: Int,
  deserialize: suspend (T) -> M,
  process: suspend (M, MillisInstant) -> Unit,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
  batchConfig: (suspend (M) -> BatchConfig?)? = null,
  batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
) {
  require((batchConfig == null) == (batchProcess == null)) {
    "batchConfig and batchProcess must be null or !null together"
  }

  val loggedDeserialize: suspend (T) -> M = { message ->
    debug { "Deserializing message: ${message.messageId}" }
    deserialize(message).also {
      trace { "Deserialized message: ${message.messageId}" }
    }
  }

  val loggedProcess: suspend (M, MillisInstant) -> Unit = { message, publishTime ->
    debug { "Processing $message" }
    process(message, publishTime)
    trace { "Processed $message" }
  }

  val loggedBatchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = batchProcess?.let {
    { messages, publishTimes ->
      debug { "Processing $messages" }
      it(messages, publishTimes)
      trace { "Processed $messages" }
    }
  }

  return when (batchProcess == null) {
    true -> this
        .process(concurrency) { transport, message ->
          loggedProcess(loggedDeserialize(message), transport.publishTime)
        }
        .acknowledge(beforeDlq)

    false -> this
        .process(concurrency) { _, message -> loggedDeserialize(message) }
        .batchBy { datum -> batchConfig?.invoke(datum) }
        .batchProcess(
            concurrency,
            { message, datum -> loggedProcess(datum, message.publishTime) },
            { messages, data -> loggedBatchProcess!!(data, messages.map { it.publishTime }) },
        )
        .acknowledge(beforeDlq)
  }
}
