package io.infinitic.tests.tasks.inMemory

import io.infinitic.messaging.api.dispatcher.Emetter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class InMemoryEmetter<T>(private val channel: Channel<T>) : Emetter<T> {
    override suspend fun send(msg: T, after: Float) {
        if (after > 0F) delay(after.toLong())
        println("Emetter: $msg")
        channel.send(msg)
    }
}
