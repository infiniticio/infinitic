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
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val consumingScope = CoroutineScope(Dispatchers.IO)

private fun addShutdownHook() {
  Runtime.getRuntime().addShutdownHook(
      Thread {
        println("\nInterrupted!\n")
        cancel()
      },
  )
}

fun cancel() = runBlocking {
  if (consumingScope.isActive) {
    consumingScope.cancel()
    // wait for all ongoing messages to be processed
    consumingScope.coroutineContext.job.children.forEach { it.join() }
    println("\nExiting after completion of ongoing messages\n")
  } else {
    println("\nExiting as canceled\n")
  }
}

/**
 * This code checks:
 * - that an Exception never stop the worker, wherever it is thrown
 * - that an Error kills the worker, but the worker still tries to process the ongoing messages
 */
fun main() {
  addShutdownHook()

  val start = Instant.now()

  runConsumer(handler, 500)

  println("Duration = ${Duration.between(start, Instant.now()).toMillis()} ms")
}

private suspend fun processPulsarMessage(
  handler: suspend (String) -> Unit,
  pulsarMessage: String,
  workerIndex: Int
) {
  val pre = "Worker ${workerIndex.format()} - message ${pulsarMessage.padEnd(5)} - "
  try {
    try {
      println("$pre Deserializing")
      randomThrowable("")
    } catch (e: Exception) {
      println("$pre Deserializing - $e ${e.stack()}")
      negativeAcknowledging("$pre Deserializing")
      return
    }

    try {
      println("$pre Processing")
      handler("$pre Processing")
    } catch (e: Exception) {
      println("$pre Processing - $e ${e.stack()}")
      negativeAcknowledging("$pre Processing")
      return
    }

    try {
      acknowledging("$pre Acknowledging")
    } catch (e: Exception) {
      println("$pre $e ${e.stack()}")
      negativeAcknowledging("$pre Processing")
      return
    }
  } catch (t: Throwable) {
    t.rethrowError("$pre in processPulsarMessage")
  }
}

private fun runConsumer(
  handler: suspend (String) -> Unit,
  concurrency: Int
) = try {
  runConsumerAsync(handler, concurrency).join()
} catch (e: CancellationException) {
  // no nothing
}

private fun runConsumerAsync(
  handler: suspend (String) -> Unit,
  concurrency: Int
): CompletableFuture<Unit> = consumingScope.future {
  val consumer = Consumer()

  // Channel is backpressure aware
  // we can use it to send messages to the executor coroutines
  val channel = Channel<String>()

  // start executor coroutines
  val jobs = List(concurrency) {
    launch {
      try {
        for (pulsarMessage in channel) {
          withContext(NonCancellable) {
            processPulsarMessage(handler, pulsarMessage, it)
          }
        }
      } catch (e: CancellationException) {
        // continue
      }
      println("worker ${it.format()} ended")
    }
  }
  // start message receiver
  while (isActive) {
    try {
      // await() is a suspendable and should be used instead of get()
      val pulsarMessage = consumer.receiveAsync().await()
      channel.send(pulsarMessage)
    } catch (e: CancellationException) {
      println("Exciting receiving loop after CancellationException")
      // if coroutine is canceled, we just exit the while loop
      break
    } catch (e: Throwable) {
      e.rethrowError("in receiveAsync")
      // for other exception, we continue
      continue
    }
  }
  // Now that the coroutine is cancelled,
  // we wait for the completion of all ongoing processPulsarMessage
  println("After cancellation, waiting completion of all ongoing messages")
  withContext(NonCancellable) { jobs.joinAll() }
  println("Closed consumer after cancellation")
}

private class Consumer() {
  val counter = AtomicInteger(0)

  fun receiveAsync(): CompletableFuture<String> = CompletableFuture.supplyAsync {
    counter.incrementAndGet().let {
      if (it == 1000) {
        println("\nWORK TERMINATED\n")
        consumingScope.cancel()
      } else {
        println("Receiving $it")
        randomThrowable("$it")
      }
      it.toString()
    }
  }
}

private suspend fun acknowledging(txt: String) {
  randomThrowable("while acknowledging $txt")
  println(txt)
  yield()
}

private suspend fun negativeAcknowledging(txt: String) {
  randomError("", 1000)
  // no exception never thrown from here
  println("$txt - negative acknowledging")
  yield()
}


private suspend fun sendMessage(txt: String) {
  randomThrowable("while sending")
  println(txt)
  yield()
}

private fun randomThrowable(
  txt: String,
  exceptionThreshold: Int = 990,
  errorThreshold: Int = 1000
) {
  randomException(txt, exceptionThreshold)
  randomError(txt, errorThreshold)
}

private fun randomException(txt: String, threshold: Int = 900) {
  if (Random.nextLong(1000) > threshold) throw RuntimeException(txt)
}

private fun randomError(txt: String, threshold: Int = 950) {
  if (Random.nextLong(1000) > threshold) throw OutOfMemoryError(txt)
}

val handler: suspend (String) -> Unit = { str: String ->
  coroutineScope {
    launch { sendMessage("$str - Handler START") }
    // emulate user-provided java execution
    //withContext(Dispatchers.Default) {
    execute()
    //}
    launch { sendMessage("$str - Handler END") }
  }
}

private fun execute() {
  Thread.sleep(Random.nextLong(100))
  randomThrowable("in execute")
}

private fun Throwable.rethrowError(txt: String) {
  val e = if (this is CompletionException) (cause ?: this) else this
  when (e) {
    is Exception -> println("$txt - Exception $e ${e.stack()}")
    else -> {
      println("$txt - Error $e ${e.stack()}")
      throw e
    }
  }
}

private fun Int.format() = String.format("%03d", this)

private fun Throwable.stack() = "\n" + stackTraceToString()
