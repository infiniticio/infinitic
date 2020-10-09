package io.infinitic.tests.tasks.inMemory

import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.messaging.api.dispatcher.Receiver
import kotlinx.coroutines.channels.Channel

open class InMemoryReceiver<T>(private val channel: Channel<T>) : Receiver<T> {
    override suspend fun onMessage(apply: (T) -> Unit) : T {
        val msg = channel.receive()
        println("Receiver: $msg")
        apply(msg)

        return msg
    }
}
