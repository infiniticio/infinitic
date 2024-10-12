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
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M : Any> Channel<Result<T, T>>.completeProcess(
  concurrency: Int,
  deserialize: suspend (T) -> M,
  process: suspend (M, MillisInstant) -> Unit,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
  batchConfig: (suspend (M) -> BatchConfig?)? = null,
  batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null,
  maxRedeliver: Int,
): Unit = this
    .process(
        concurrency,
        { _, message -> deserialize(message) },
    )
    .batchBy { datum ->
      batchConfig?.invoke(datum)
    }
    .batchProcess(
        concurrency,
        { message, datum -> process(datum, message.publishTime); datum },
        { messages, data -> batchProcess!!(data, messages.map { it.publishTime }); data },
    )
    .collect(maxRedeliver, beforeDlq)


