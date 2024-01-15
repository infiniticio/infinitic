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

@Serializable(with = VersionSerializer::class)
data class Version(val versionString: String) : Comparable<Version> {

  override fun toString() = versionString

  override fun compareTo(other: Version): Int {
    val (major, minor, patch) = parts
    val (otherMajor, otherMinor, otherPatch) = other.parts

    return when {
      major != otherMajor -> major - otherMajor
      minor != otherMinor -> minor - otherMinor
      else -> patch - otherPatch
    }
  }

  @Suppress("UNCHECKED_CAST")
  private val parts
    get() = versionString.removeSuffix("-SNAPSHOT").split(".").map { it.toIntOrNull() }
        .also { parts ->
          if (parts.size != 3 || parts.any { it == null }) {
            throw IllegalArgumentException("Invalid version string: $versionString")
          }
        } as List<Int>

}

object VersionSerializer : KSerializer<Version> {
  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("Version", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Version) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder) = Version(decoder.decodeString())
}
