/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MillisDurationSerializer::class)
data class MillisDuration(val long: Long) : Comparable<Long> {
    override fun toString() = "$long"

    override operator fun compareTo(other: Long): Int = this.long.compareTo(other)

    operator fun plus(other: MillisDuration) = MillisDuration(this.long + other.long)

    operator fun minus(other: MillisDuration) = MillisDuration(this.long - other.long)

    operator fun plus(other: MillisInstant) = MillisInstant(this.long + other.long)

    companion object {
        val ZERO = MillisDuration(0)
    }
}

object MillisDurationSerializer : KSerializer<MillisDuration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MillisDuration", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: MillisDuration) { encoder.encodeLong(value.long) }
    override fun deserialize(decoder: Decoder) = MillisDuration(decoder.decodeLong())
}
