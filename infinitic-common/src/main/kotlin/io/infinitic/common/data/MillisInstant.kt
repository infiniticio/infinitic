/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.data

import java.time.Instant as JavaInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MillisInstantSerializer::class)
data class MillisInstant(val long: Long = 0) : Comparable<Long> {
  companion object {
    fun now() = MillisInstant(JavaInstant.now().toEpochMilli())
  }

  override fun toString() = "$long"

  override operator fun compareTo(other: Long): Int = this.long.compareTo(other)

  operator fun plus(other: MillisDuration) = MillisInstant(this.long + other.long)

  operator fun minus(other: MillisDuration) = MillisInstant(this.long - other.long)

  operator fun minus(other: MillisInstant) = MillisDuration(this.long - other.long)
}

object MillisInstantSerializer : KSerializer<MillisInstant> {
  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("MillisInstant", PrimitiveKind.LONG)
  override fun serialize(encoder: Encoder, value: MillisInstant) {
    encoder.encodeLong(value.long)
  }
  override fun deserialize(decoder: Decoder) = MillisInstant(decoder.decodeLong())
}
