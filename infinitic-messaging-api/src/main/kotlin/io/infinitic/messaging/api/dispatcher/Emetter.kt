package io.infinitic.messaging.api.dispatcher

interface Emetter<T> {
    suspend fun send(msg: T, after: Float = 0F)
}
