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
