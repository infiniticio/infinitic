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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val otherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val consumingScope = CoroutineScope(Dispatchers.IO)

private val producingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun <T> consume(func: suspend () -> T): T =
    consumingScope.future { func() }.join()

private fun <T> produce(func: suspend () -> T): T =
    producingScope.future { func() }.join()

/**
 * This code checks:
 * - that a ERROR thrown kills the worker
 * - that an Exception thrown is well handled, and does not kill the producingScope
 */
fun main() {
  val handler = { _: String ->
    produce {
      val r = Random.nextLong(1000)
      delay(r)
      // if (r > 990) throw OutOfMemoryError("dead")
      if (r > 900) throw RuntimeException("oops")
    }
  }

  consume { startConsumer(handler) }
}

private fun Throwable.rethrowError() {
  val e = if (this is CompletionException) cause else this
  if (e != null && e !is Exception) throw e
}

private fun processPulsarMessage(
  handler: (String) -> Unit,
  pulsarMessage: String,
  workerIndex: Int
) {
  println("Worker $workerIndex: start($pulsarMessage)")
  try {
    handler(pulsarMessage)
  } catch (e: Throwable) {
    println("Worker $workerIndex: Exception ${e.cause} when processing $pulsarMessage")
    e.rethrowError()
    return
  }
  println("Worker $workerIndex: end  ($pulsarMessage)")
}

private suspend fun startConsumer(
  handler: (String) -> Unit
) = coroutineScope {
  val consumer = Consumer()
  val concurrency = 100

  // Channel is backpressure aware
  // we can use it to send messages to the executor coroutines
  val channel = Channel<String>()

  // start executor coroutines
  val jobs = List(concurrency) {
    launch {
      try {
        for (pulsarMessage in channel) {
          processPulsarMessage(handler, pulsarMessage, it)
        }
      } catch (e: CancellationException) {
        // exiting
      }
      println("worker $it ended")
    }
  }
  // start message receiver
  while (isActive) {
    try {
      // await() is a suspendable and should be used instead of get()
      val pulsarMessage = consumer.receiveAsync().await()
      channel.send(pulsarMessage)
    } catch (e: CancellationException) {
      // if coroutine is canceled, we just exit the while loop
      break
    } catch (e: Exception) {
      // for other exception, we continue
      println("Exception $e in receiveAsync")
      continue
    }
  }
  println("Closing consumer after cancellation")
  withContext(NonCancellable) { jobs.joinAll() }
  println("Closed consumer after cancellation")
}

private class Consumer() {
  val counter = AtomicInteger(0)

  fun receiveAsync(): CompletableFuture<String> = otherScope.future {
    val r = Random.nextLong(1000)
    // if (r > 990) throw OutOfMemoryError("coming from receiveAsync")
    //if (r > 900) throw RuntimeException("coming from receiveAsync")
    counter.incrementAndGet().toString()
  }
}

