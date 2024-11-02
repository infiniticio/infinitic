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
import io.infinitic.common.transport.BatchProcessorConfig
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Starts consuming transport messages asynchronously with the given parameters.
 *
 * @param concurrency The number of concurrent coroutines for processing messages.
 * @param deserialize A suspending function to deserialize the transport message into its payload.
 * @param processor A suspending function to process the deserialized message along with its publishing time.
 * @param batchProcessorConfig An optional suspending function to configure batching of messages.
 * @param batchProcessor An optional suspending function to process a batch of messages.
 * @return A Job representing the coroutine that runs the consuming process.
 */
context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M : Any> TransportConsumer<T>.startAsync(
  batchReceivingConfig: BatchConfig?,
  concurrency: Int,
  deserialize: suspend (T) -> M,
  processor: suspend (M, MillisInstant) -> Unit,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
  batchProcessorConfig: (suspend (M) -> BatchProcessorConfig?)? = null,
  batchProcessor: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null,
): Job = launch {
  startConsuming(batchReceivingConfig != null)
      .completeProcess(
          concurrency,
          deserialize,
          processor,
          beforeDlq,
          batchProcessorConfig,
          batchProcessor,
      )
}
