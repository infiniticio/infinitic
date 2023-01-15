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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = NameSerializer::class)
open class Name(open val name: String) : CharSequence by name, Comparable<String> by name {
  final override fun toString() = name

  fun get() = name

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Name

    return name == other.name
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}

object NameSerializer : KSerializer<Name> {
  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("Name", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Name) {
    encoder.encodeString(value.name)
  }
  override fun deserialize(decoder: Decoder) = Name(decoder.decodeString())
}
