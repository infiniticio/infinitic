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

package io.infinitic.common.utils

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.UUIDType
import com.fasterxml.uuid.impl.UUIDUtil
import io.infinitic.common.data.MillisInstant
import java.security.MessageDigest

object IdGenerator {
  private val generator = Generators.timeBasedEpochGenerator()

  /**
   * Generates the next string using the generator and returns it as a string.
   *
   * @return The generated string.
   */
  @JvmStatic
  fun next(): String = generator.generate().toString()

  /**
   * Deterministically constructs a UUID v7 string using the given [instant] and a String [seedString]
   * hashed to seed the random part of the uuid
   *
   * @param instant The MillisInstant representing the time component of the UUID.
   * @param seedString The string used to calculate the entropy component of the UUID.
   * @return The generated UUID string.
   */
  @JvmStatic
  fun from(instant: MillisInstant, seedString: String): String {
    val entropy = seedString.toEntropy()

    return UUIDUtil.constructUUID(
        UUIDType.TIME_BASED_EPOCH,
        (instant.long shl 16) or toShort(entropy),
        toLong(entropy),
    ).toString()
  }

  private fun toLong(buffer: ByteArray): Long {
    val l1 = toInt(buffer, 2)
    val l2 = toInt(buffer, 6)
    val l = (l1 shl 32) + ((l2 shl 32) ushr 32)
    return l
  }

  private fun toShort(buffer: ByteArray): Long =
      (((buffer[0].toInt() and 0xFF) shl 8) + (buffer[1].toInt() and 0xFF)).toLong()

  private fun toInt(buffer: ByteArray, offset: Int): Long = (
      (buffer[offset].toInt() shl 24)
          + ((buffer[offset + 1].toInt() and 0xFF) shl 16)
          + ((buffer[offset + 2].toInt() and 0xFF) shl 8)
          + (buffer[offset + 3].toInt() and 0xFF)
      ).toLong()

  private fun String.toEntropy(): ByteArray {
    // https://stackoverflow.com/questions/17554998/need-thread-safe-messagedigest-in-java
    val hashedBytes = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))

    return when {
      hashedBytes.size >= 10 -> hashedBytes.copyOfRange(0, 10)
      else -> ByteArray(10).also { hashedBytes.copyInto(it) }
    }
  }
}

