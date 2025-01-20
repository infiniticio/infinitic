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
package io.infinitic.clients.scaffold

import io.infinitic.clients.dispatcher.ResponseFlow
import io.kotest.common.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean


private class Client {
  private val scope = CoroutineScope(Dispatchers.IO)
  private val responseFlow = ResponseFlow<String>()
  private val isStarted = AtomicBoolean(false)
  var msg = 0

  private fun startAsync(): CompletableFuture<Unit> = scope.future {
    try {
      while (isActive) {
        msg++
        delay(5)
        responseFlow.emit(msg.toString())
        if (msg == 150) throw Exception("Breaking!")
      }
    } catch (e: Exception) {
      responseFlow.emitThrowable(e)
      throw e
    }
  }

  fun await(timeout: Long = Long.MAX_VALUE, predicate: (String) -> Boolean): String? = runBlocking {
    if (isStarted.compareAndSet(false, true)) {
      startAsync()
    }
    responseFlow.first(timeout) { predicate(it) }
  }
}

suspend fun main() {
  val client = Client()
  println("continue")
  client.await { it.toLong() == 1L }.also { println("Found $it") }
  coroutineScope {
    launch {
      client.await { it.toLong() >= 100 }.also { println("Found $it") }
    }
    launch {
      try {
        client.await { it.toLong() >= 200 }.also { println("Found $it") }
      } catch (e: Exception) {
        println("Found $e")
      }
    }
    launch {
      client.await { it.toLong() >= 30000 }.also { println("Found $it") }
    }
  }

  println("joined")
}
