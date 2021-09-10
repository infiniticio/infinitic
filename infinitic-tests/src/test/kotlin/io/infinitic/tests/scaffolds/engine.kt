/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tests.scaffolds

import io.infinitic.inMemory.transport.InMemoryMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.yield
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * This code checks:
 * - that a messages is pulled only when it can be handled
 * - that events are processed before commands
 * - that uncaught exception will terminate the app
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

fun CoroutineScope.startEngine() {
    val concurrency = 10
    val eventsNb = 100
    val commandsNb = 100
    val waiting = 300L

    // 1  PER CONCURRENCY
    repeat(concurrency) {

        val eventChannel: Channel<InMemoryMessageToProcess<String>> = Channel()
        val commandChannel: Channel<InMemoryMessageToProcess<String>> = Channel()

        launch {
            while (isActive) {
                select<Unit> {
                    eventChannel.onReceive {
                        println("    receiving: ${it.message}")
                        val out = Random.nextLong(500)
                        Thread.sleep(waiting)
                        it.returnValue = out
                        println("    acknowledging: ${it.message}")
                    }
                    commandChannel.onReceive {
                        println("    receiving: ${it.message}")
                        val out = Random.nextLong(500)
                        Thread.sleep(waiting)
                        it.returnValue = out
                        println("    acknowledging: ${it.message}")
                    }
                }
            }
        }

        // ONE KEY_SHARED CONSUMER / THREAD
        launch {
            repeat(commandsNb) {
                val command = "command-$it"
                println("sending... $command")
                commandChannel.send(InMemoryMessageToProcess(command))
                yield()
            }
        }
        // ONE KEY_SHARED CONSUMER / THREAD
        launch {
            repeat(eventsNb) {
                val event = "event-$it"
                println("sending... $event")
                eventChannel.send(InMemoryMessageToProcess(event))
                yield()
            }
        }
    }

    launch {
        delay(1000)
//        throw Exception("Breaking!")
    }
}
