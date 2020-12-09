package io.infinitic.pulsar.workers

import io.infinitic.common.workers.MessageToProcess
import org.apache.pulsar.client.api.MessageId

data class PulsarMessageToProcess<T> (
    override val message: T,
    val messageId: MessageId
) : MessageToProcess<T> {
    override var exception: Exception? = null
    override var output: Any? = null
}
