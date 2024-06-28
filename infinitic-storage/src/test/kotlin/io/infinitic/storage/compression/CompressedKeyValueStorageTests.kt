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

import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.infinitic.storage.keyValue.KeyValueStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class CompressedKeyValueStorageTests :
  StringSpec(
      {
        val keyValueStorage = mockk<KeyValueStorage>()
        coEvery { keyValueStorage.get("foo") } returns "bar".toByteArray()
        Compression.entries.forEach {
          coEvery { keyValueStorage.get(it.toString()) } returns it.compress("bar".toByteArray())
        }
        val bytes = slot<ByteArray>()
        coEvery { keyValueStorage.put("foo", capture(bytes)) } just Runs

        // ensure slot is emptied between each test
        beforeTest { bytes.clear() }

        "get returns the uncompressed value, independently of the compressor used" {
          Compression.entries.forEach { whatEverCompressor ->
            val compressedStorage = CompressedKeyValueStorage(whatEverCompressor, keyValueStorage)
            compressedStorage.get("foo")!!.contentEquals("bar".toByteArray()) shouldBe true
          }
        }

        "get returns a value decompressed, independently of the compressor used" {
          Compression.entries.forEach { whatEver ->
            val compressedStorage = CompressedKeyValueStorage(whatEver, keyValueStorage)
            Compression.entries.forEach {
              compressedStorage.get(it.toString())!!
                  .contentEquals("bar".toByteArray()) shouldBe true
            }
          }
        }

        "put stores a value compressed with the compressor provided" {
          Compression.entries.forEach { compressor ->
            val compressedStorage = CompressedKeyValueStorage(compressor, keyValueStorage)
            compressedStorage.put("foo", "bar".toByteArray())
            bytes.isCaptured shouldBe true
            bytes.captured.contentEquals(compressor.compress("bar".toByteArray())) shouldBe true
          }
        }

        "put stores a value uncompressed if no compressor is provided" {
          val compressedStorage = CompressedKeyValueStorage(null, keyValueStorage)
          compressedStorage.put("foo", "bar".toByteArray())
          bytes.isCaptured shouldBe true
          bytes.captured.contentEquals("bar".toByteArray()) shouldBe true
        }
      },
  )
