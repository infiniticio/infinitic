package io.infinitic.taskManager.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed class CounterMsg {
    object IncCounter : CounterMsg() // one-way message to increment counter
    class GetCounter(val response: SendChannel<Int>) : CounterMsg() // a request with channel for reply.
}

@ObsoleteCoroutinesApi
fun counterActor() = GlobalScope.actor<CounterMsg> {
    var counter = 0 // (9) </b>actor state, not shared
    for (msg in channel) { // handle incoming messages
        when (msg) {
            is CounterMsg.IncCounter -> counter++ // (4)
            is CounterMsg.GetCounter -> msg.response.send(counter) // (3)
        }
    }
}

suspend fun getCurrentCount(counter: SendChannel<CounterMsg>): Int { // (8)
    val response = Channel<Int>() // (2)
    counter.send(CounterMsg.GetCounter(response))
    val receive = response.receive()
    println("Counter = $receive")
    return receive
}

@ObsoleteCoroutinesApi
fun main(args: Array<String>) = runBlocking<Unit> {
    val counter = counterActor()

    GlobalScope.launch { // (5)
        while (getCurrentCount(counter) < 100) {
            delay(100)
            println("sending IncCounter message")
            counter.send(CounterMsg.IncCounter) // (7)
        }
    }

    GlobalScope.launch { // (6)
        while (getCurrentCount(counter) < 100) {
            delay(200)
        }
    }.join()

    counter.close() // shutdown the actor
}
