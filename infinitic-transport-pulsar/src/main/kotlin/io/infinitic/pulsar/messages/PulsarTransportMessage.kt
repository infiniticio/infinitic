package io.infinitic.pulsar.messages

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.TransportMessage
import org.apache.pulsar.client.api.Messages
import org.apache.pulsar.client.api.Message as PulsarMessage

class PulsarTransportMessage<E>(private val pulsarMessage: PulsarMessage<E>) : TransportMessage {
  override val messageId: String = pulsarMessage.messageId.toString()
  override val redeliveryCount: Int = pulsarMessage.redeliveryCount
  override val publishTime: MillisInstant = MillisInstant(pulsarMessage.publishTime)
  internal fun toPulsarMessage() = pulsarMessage
}

fun <E : Envelope<out M>, M : Message> deserialize(message: PulsarTransportMessage<E>): M =
    message.toPulsarMessage().value.message()

internal class PulsarMessages<E>(val messages: List<PulsarMessage<E>>) : Messages<E> {
  override fun iterator() = messages.toMutableList().iterator()
  override fun size() = messages.size
}

internal fun <E> List<PulsarTransportMessage<E>>.toPulsarMessages() =
    PulsarMessages(map { it.toPulsarMessage() })
