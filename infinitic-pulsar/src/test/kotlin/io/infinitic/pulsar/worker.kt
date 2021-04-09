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

    // WORKER INSTANCES
    repeat(10) {
        thread {
            runBlocking {
                while (isActive) {
                    select<Unit> {
                        eventChannel.onReceive {
                            println("    receiving: ${it.message}")
                            val out = Random.nextLong(5000)
                            Thread.sleep(out)
                            it.returnValue = out
                            println("    done: ${it.message}")
                            outChannel.send(it)
                        }
                        commandChannel.onReceive {
                            println("    receiving: ${it.message}")
                            val out = Random.nextLong(5000)
                            Thread.sleep(out)
                            it.returnValue = out
                            println("    done: ${it.message}")
                            outChannel.send(it)
                        }
                    }
                }
            }
        }
    }

    thread {
        runBlocking(Dispatchers.IO) {
            // COMMAND CONSUMER
            launch {
                repeat(100) {
                    val command = "command-$it"
                    println("sending... $command")
                    commandChannel.send(InMemoryMessageToProcess(command))
                    yield()
                }
            }
            // EVENT CONSUMER
            launch {
                repeat(100) {
                    val event = "event-$it"
                    println("sending... $event")
                    eventChannel.send(InMemoryMessageToProcess(event))
                    yield()
                }
            }
            // ACKNOWLEDGMENT
            while (isActive) {
                val msg = outChannel.receive()
                println("    acknowledging: $msg")
            }
        }
    }

    Unit
}
