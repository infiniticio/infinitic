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
package io.infinitic.common.transport.consumer

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.exceptions.thisShouldNotHappen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val producersMutex = Mutex()
private val producerCounters = mutableMapOf<Channel<*>, Int>()

context(KLogger)
suspend fun Channel<*>.addProducer() = producersMutex.withLock {
  debug { "Adding one producer from ${producerCounters[this]} to channel ${this.hashCode()}" }
  producerCounters[this] = (producerCounters[this] ?: 0) + 1
}

context(KLogger)
suspend fun Channel<*>.removeProducer() = producersMutex.withLock {
  debug { "Removing one producer from ${producerCounters[this]} from channel ${this.hashCode()}" }
  when (val count = producerCounters[this]) {
    null -> thisShouldNotHappen()
    1 -> {
      producerCounters.remove(this)
      debug { "closing channel ${this.hashCode()}" }
      this.close()
    }

    else -> producerCounters[this] = (count - 1)
  }
}
