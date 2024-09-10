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
package io.infinitic.workers.scafold

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.exitProcess

private suspend fun processMessage(msg: String) {
  println(">> processing $msg")
  val d = Random.nextLong(1000)
  when {
    //d < 50 -> throw RuntimeException("error $msg")
    else -> delay(d)
  }
  println(">>>> processed $msg")
}

private class ConsumerWithoutKey {

  suspend fun start(concurrency: Int) = coroutineScope {
    val channel = Channel<String>()
    // start executor coroutines
    val jobs = List(concurrency) {
      launch {
        try {
          for (msg: String in channel) {
            // this ensures that ongoing messages are processed
            // even after scope is cancelled following an interruption or an Error
            withContext(NonCancellable) {
              processMessage(msg)
            }
          }
        } catch (e: CancellationException) {
          println("Processor #$it closed after cancellation")
        } catch (e: Exception) {
          println("Processor #$it closed after error $e")
          println("Waiting processing of ongoing messages")
          throw e
        }
      }
    }
    // start message receiver
    var msg = 0
    while (isActive) {
      try {
        msg++
        println("receiving $msg")
        channel.send(msg.toString())
      } catch (e: CancellationException) {
        // if current scope  is canceled, we just exit the while loop
        break
      }
    }
    withContext(NonCancellable) { jobs.joinAll() }
    println("Closing consumer")
  }
}

private class ConsumerWithKey {

  suspend fun start(concurrency: Int) = coroutineScope {
    val msg = AtomicInteger(0)
    // start executor coroutines
    // For Key_Shared subscription, we must create a new consumer for each executor coroutine
    List(concurrency) { consumer ->
      launch {
        while (isActive) {
          try {
            msg.addAndGet(1)
            println("receiving Consumer$consumer: $msg")
            // this ensures that ongoing messages are processed
            // even after scope is cancelled following an interruption or an Error
            withContext(NonCancellable) {
              processMessage("Consumer$consumer: $msg")
            }
          } catch (e: CancellationException) {
            println("Exiting receiving loop $consumer")
            // if current scope is canceled, we just exit the while loop
            throw e
          } catch (e: Exception) {
            println("Closing consumer $consumer after Exception $e")
            throw e
          }
        }
        println("Closing consumer $consumer")
      }
    }
  }
}

private class Worker {
  private val scope = CoroutineScope(Dispatchers.IO)
  private val consumerWithoutKey = ConsumerWithoutKey()
  private val consumerWithKey = ConsumerWithKey()
  private var isClosed: AtomicBoolean = AtomicBoolean(false)

  init {
    Runtime.getRuntime().addShutdownHook(
        Thread {
          close()
        },
    )
  }

  fun CoroutineScope.startWithoutKey(concurrency: Int) {
    launch {
      println("consumerWithoutKey started")
      try {
        consumerWithoutKey.start(5)
      } catch (e: CancellationException) {
        // do nothing
      }
      println("consumerWithoutKey stopped")
    }
  }

  fun CoroutineScope.startWithKey(concurrency: Int) {
    launch {
      println("consumerWithKey started")
      try {
        consumerWithKey.start(5)
      } catch (e: CancellationException) {
        // do nothing
      }
      println("consumerWithKey stopped")
    }
  }


  fun startAsync() = scope.future {
    println("a")
    //startWithoutKey(5)
    startWithKey(5)
    println("b")
  }

  fun start() {
    try {
      startAsync().join()
    } catch (e: CancellationException) {
      // do nothing
    } catch (e: Exception) {
      println(e.printStackTrace())
      exitProcess(1)
    }
  }

  fun close() {
    if (isClosed.compareAndSet(false, true)) {
      println("Closing worker...")
      scope.cancel()
      runBlocking {
        try {
          withTimeout(1000) {
            scope.coroutineContext.job.children.forEach { it.join() }
            println("all message processed")
          }
        } catch (e: TimeoutCancellationException) {
          println("The grace period allotted to close was insufficient.")
        }
      }
      println("Worker closed.")
    }
  }
}

suspend fun main() {
  val worker = Worker()
//  later(10000) {
//    worker.close()
//  }
  worker.startAsync()
  delay(1000)
  println("That's all folks")
}
