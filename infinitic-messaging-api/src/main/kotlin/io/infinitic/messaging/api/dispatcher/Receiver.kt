package io.infinitic.messaging.api.dispatcher

interface Receiver<T> {
    suspend fun onMessage(apply: (T) -> Unit) : T
}
