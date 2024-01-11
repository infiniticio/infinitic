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

package io.infinitic.pulsar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

private val consumingScope = CoroutineScope(Dispatchers.IO)
private val producingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var counter = 0
private var atomicCounter = AtomicInteger(0)

fun main(): Unit {
  val start = Instant.now()
  try {
    startConsumer(concurrency = 500)
  } catch (e: RuntimeException) {
    println(e)
  }
  val duration = Duration.between(start, Instant.now())
  println("Duration = ${duration.toMillis()}ms")
}

private fun processMessage(
  pulsarMessage: String,
  workerIndex: Int
) = producingScope.future {
  println("Worker $workerIndex: start($pulsarMessage)")
  delay(100)
  println("Worker $workerIndex: end  ($pulsarMessage)")
  atomicCounter.incrementAndGet().let {
    if (it == 1000) throw RuntimeException()
  }
}.join()

private fun startConsumer(concurrency: Int) = consumingScope.future {
  val channel = Channel<String>()

  // start processing coroutines
  repeat(concurrency) {
    launch {
      for (message in channel) {
        processMessage(message, it)
      }
    }
  }
  // loop sending messages to the channel
  while (isActive) {
    counter++
    channel.send("$counter")
  }
}.join()
