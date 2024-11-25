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
package io.infinitic.storage.databases.inMemory

import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.data.Bytes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class InMemoryKeySetStorageTests :
  StringSpec(
      {
        val config = InMemoryConfig()
        val storage = InMemoryKeySetStorage.from(config)

        beforeTest { storage.add("foo", "bar".toByteArray()) }

        afterTest { storage.flush() }

        fun equalsTo(set1: Set<ByteArray>, set2: Set<ByteArray>) =
            set1.map { Bytes(it) }.toSet() == set2.map { Bytes(it) }.toSet()

        "get should return an empty set on unknown key" {
          storage.get("unknown") shouldBe setOf<ByteArray>()
        }

        "get should return the set on known key" {
          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "add on unknown key should create a new set" {
          storage.add("unknown", "42".toByteArray())

          equalsTo(storage.get("unknown"), setOf("42".toByteArray())) shouldBe true
        }

        "add on known key should add to set" {
          storage.add("foo", "42".toByteArray())

          equalsTo(storage.get("foo"), setOf("42".toByteArray(), "bar".toByteArray())) shouldBe true
        }

        "add known value on known key should do nothing" {
          storage.add("foo", "bar".toByteArray())

          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "remove on unknown key should do nothing" {
          storage.remove("unknown", "42".toByteArray())
        }

        "remove unknown value on known key should do nothing" {
          storage.remove("foo", "42".toByteArray())

          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "remove known value on known key should remove from set" {
          storage.remove("foo", "bar".toByteArray())

          equalsTo(storage.get("foo"), setOf()) shouldBe true
        }

        "get on multiple keys" {
          storage.add("foo", "42".toByteArray())
          val gets = storage.get(setOf("foo", "unknown"))
          gets.keys shouldBe setOf("foo", "unknown")
          gets["foo"] shouldContainExactly listOf("bar".toByteArray(), "42".toByteArray())
          gets["unknown"] shouldBe setOf<ByteArray>()
        }

        "update on multiple keys" {
          storage.update(
              add = mapOf(
                  "foo" to setOf("42".toByteArray(), "43".toByteArray()),
                  "bar" to setOf("43".toByteArray()),
              ),
              remove = mapOf(
                  "foo" to setOf("bar".toByteArray(), "unknown".toByteArray()),
                  "unknown" to setOf("42".toByteArray()),
              ),
          )
          storage.get("foo") shouldContainExactly listOf("42".toByteArray(), "43".toByteArray())
          storage.get("bar") shouldContainExactly listOf("43".toByteArray())
        }
      },
  )
