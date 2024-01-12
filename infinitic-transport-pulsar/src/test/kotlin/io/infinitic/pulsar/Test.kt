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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val consumingScope = CoroutineScope(Dispatchers.IO)
private var counter = 0
private var atomicCounter = AtomicInteger(0)

fun main() {
  Runtime.getRuntime().addShutdownHook(
      Thread {
        println("\nInterrupted!\n")
        runBlocking {
          consumingScope.cancel()
          // wait for all ongoing messages to be processed
          consumingScope.coroutineContext.job.children.forEach { it.join() }
        }
        println("\nExiting\n")
      },
  )

  val start = Instant.now()
  try {
    startConsumer(concurrency = 500)
  } catch (e: RuntimeException) {
    println("Stopped with $e")
  }
  val duration = Duration.between(start, Instant.now())
  println("Duration = ${duration.toMillis()}ms")
}

private fun startConsumer(concurrency: Int) = consumingScope.future {
  val channel = Channel<String>()

  // start processing coroutines

  repeat(concurrency) {
    launch {
      for (message in channel) {
        withContext(NonCancellable) {
          processMessage(message, it)
        }
      }
    }
  }
  // loop sending messages to the channel
  while (isActive) {
    counter++
    channel.send("$counter")
    randomException("error while receiving message")
  }
}.join()

private suspend fun processMessage(
  pulsarMessage: String,
  workerIndex: Int
) = coroutineScope {
  sendMessage("Worker $workerIndex: start($pulsarMessage)")

  // emulate java execution
  withContext(Dispatchers.Default) {
    Thread.sleep(10)
    randomException("error while processing message")
    atomicCounter.incrementAndGet().let {
      sendMessage("Worker $workerIndex: proce($pulsarMessage - $it)")
      if (it == 10000) {
        throw RuntimeException()
        // we check here that all ongoing messages are processed correctly before leaving
      }
    }
  }

  sendMessage("Worker $workerIndex: end  ($pulsarMessage)")
}

private fun CoroutineScope.sendMessage(txt: String) = launch {
  println(txt)
  randomException("error while sending message")
}

private fun randomException(txt: String) {
  val r = Random.nextLong(1000)
  if (r > 950) throw RuntimeException(txt)
}
