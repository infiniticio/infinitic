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
package io.infinitic.storage.compression

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class CompressorTests :
  StringSpec(
      {
        val charPool: List<Char> = ('Z' downTo '0').toList()

        fun randomString(length: Int) =
            (1..length).map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("")

        fun getBytes(length: Int = 1000): ByteArray {
          val str = randomString(length)
          return str.toByteArray()
        }

        fun getBytesWithCompressorHeader(
          compression: Compression,
          header: Int = 300,
          length: Int = 1000
        ): ByteArray {
          val bytes = getBytes(length)
          return compression.compress(bytes).copyOf(header) + getBytes(length - header)
        }

        fun testDecompressing(original: ByteArray, compressed: ByteArray) {
          val decompressed = Compression.decompress(compressed)
          decompressed.contentEquals(original) shouldBe true
        }

        "Decompressing uncompressed data should return the same data" {
          val data = getBytes()
          testDecompressing(data, data)
        }

        "Decompressing uncompressed data should return the same data, even with a compressor header" {
          Compression.entries.forEach {
            val data = getBytesWithCompressorHeader(it)
            testDecompressing(data, data)
          }
        }

        "Decompressing compressed data should return the same data" {
          Compression.entries.forEach {
            val data = getBytes()
            val compressed = it.compress(data)
            testDecompressing(data, compressed)
          }
        }

        "compressed data should be compressed" {
          Compression.entries.forEach {
            val data = getBytes()
            val compressed = it.compress(data)
            compressed.size shouldBeLessThan data.size
          }
        }
      },
  )
