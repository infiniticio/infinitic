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
package io.infinitic.tests.scaffolds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * This code checks:
 * - that a messages is pulled only when it can be handled
 * - that uncaught exception will terminate the app
 * - that there is no deadlock when back messages are not synchronous
 */
fun main() {
  val threadPool = Executors.newCachedThreadPool()
  val scope = CoroutineScope(threadPool.asCoroutineDispatcher())

  scope.launch {
    try {
      coroutineScope { startEngine() }
    } catch (e: Throwable) {
      threadPool.shutdown()
    }
  }
}

private fun <T> CoroutineScope.startEngine(
  executor: suspend (T) -> Unit,
  channel: Channel<T>
): Job = launch {
  for (message in channel) {
    try {
      executor(message)
    } catch (e: Throwable) {
      println("Error while processing message $message: \n$e")
    }
  }
}

suspend fun wait() = delay((Math.random() * 10L).toLong())

fun CoroutineScope.startEngine() {
  val msgNb = 1000

  val workflowChannel = Channel<String>()
  val taskChannel = Channel<String>()
  val executorChannel = Channel<String>()

  val workflowEngine: suspend (String) -> Unit = { message: String ->
    println("A executing $message")
    wait()
    if (!message.startsWith("taskEngine")) {
      taskChannel.send(message)
    }
    println("A executed $message")
  }

  val taskEngine: suspend (String) -> Unit = { message: String ->
    println("B executing $message")
    wait()
    if (!message.startsWith("taskExecutor")) {
      executorChannel.send(message)
    } else {
      launch { workflowChannel.send("taskEngine: $message") }
    }
    println("B executed $message")
  }

  val taskExecutor: suspend (String) -> Unit = { message: String ->
    println("C executing $message")
    wait()
    launch { taskChannel.send("taskExecutor: $message") }
    println("C executed $message")
  }

  startEngine(workflowEngine, workflowChannel)
  startEngine(taskEngine, taskChannel)
  startEngine(taskExecutor, executorChannel)

  // Sending messages
  launch {
    repeat(msgNb) {
      val msg = "message-$it"
      println("sending... $msg")
      workflowChannel.send(msg)
    }
  }

  launch {
    delay(1000)
    //        throw Exception("Breaking!")
  }
}
