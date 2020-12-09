package io.infinitic.worker.inMemory

import io.infinitic.common.workers.MessageToProcess

data class InMemoryMessageToProcess<T> (
    override val message: T
) : MessageToProcess<T> {
    override var exception: Exception? = null
    override var output: Any? = null
}
