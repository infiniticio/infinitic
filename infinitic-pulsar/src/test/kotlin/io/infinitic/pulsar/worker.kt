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
