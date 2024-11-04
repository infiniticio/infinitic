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
package io.infinitic.common.transport.config

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.transport.BatchProcessorConfig

data class BatchConfig(
  val maxMessages: Int = 1000,
  val maxSeconds: Double = 1.0
) {
  init {
    require(maxMessages > 0) { error("'${::maxMessages.name}' must be > 0, but was $maxMessages") }
    require(maxSeconds > 0) { error("'${::maxSeconds.name}' must be > 0, but was $maxSeconds") }
  }
}

fun BatchConfig?.normalized(concurrency: Int) =
    this?.copy(maxMessages = (maxMessages / concurrency).coerceAtLeast(1))

fun BatchConfig?.normalized(key: String, concurrency: Int = 1) = this?.let {
  BatchProcessorConfig(
      key,
      (maxMessages / concurrency).coerceAtLeast(1),
      MillisDuration(maxMillis),
  )
}

val BatchConfig.maxMillis: Long
  get() = (maxSeconds * 1000).toLong()
