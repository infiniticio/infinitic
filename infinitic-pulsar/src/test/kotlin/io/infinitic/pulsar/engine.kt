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

package io.infinitic.pulsar

import io.infinitic.inMemory.transport.InMemoryMessageToProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.yield
import kotlin.concurrent.thread
import kotlin.random.Random

fun main() = runBlocking {

    val eventChannel: Channel<InMemoryMessageToProcess<String>> = Channel()
    val commandChannel: Channel<InMemoryMessageToProcess<String>> = Channel()
    val outChannel: Channel<InMemoryMessageToProcess<String>> = Channel()

    // 1 COMMAND CONSUMER 1 EVENT CONSUMER PER ENGINE INSTANCE
    thread {
        runBlocking(Dispatchers.IO) {
            launch {
                while (isActive) {
                    val msg = outChannel.receive()
                    println("    acknowledging: $msg")
                }
            }
            launch {
                repeat(100) {
                    // KEY_SHARED CONSUMER
                    val command = "command-$it"
                    println("sending... $command")
                    commandChannel.send(InMemoryMessageToProcess(command))
                    yield()
                }
            }
            launch {
                repeat(100) {
                    // KEY_SHARED CONSUMER
                    val event = "event-$it"
                    println("sending... $event")
                    eventChannel.send(InMemoryMessageToProcess(event))
                    yield()
                }
            }
            while (isActive) {
                select<Unit> {
                    eventChannel.onReceive {
                        println("    receiving: ${it.message}")
                        val out = Random.nextLong(500)
                        Thread.sleep(out)
                        it.returnValue = out
                        println("    done: ${it.message}")
                        outChannel.send(it)
                    }
                    commandChannel.onReceive {
                        println("    receiving: ${it.message}")
                        val out = Random.nextLong(500)
                        Thread.sleep(out)
                        it.returnValue = out
                        println("    done: ${it.message}")
                        outChannel.send(it)
                    }
                }
            }
        }
    }

    Unit
}
