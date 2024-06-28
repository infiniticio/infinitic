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
package io.infinitic.cache.caches.caffeine

import io.infinitic.cache.Caffeine
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CaffeineCachedKeyValueTests :
  StringSpec(
      {
        val storage = CaffeineCachedKeyValue<ByteArray>(Caffeine())

        val known = "foo"
        val unknown = "bar"

        beforeTest { storage.putValue(known, known.toByteArray()) }

        afterTest { storage.flush() }

        "getValue should return null on unknown key" {
          storage.getValue(unknown) shouldBe null
        }

        "getValue should return value on known key" {
          storage.getValue(known) shouldBe known.toByteArray()
        }

        "delValue should do nothing on unknown key" {
          shouldNotThrowAny { storage.delValue(unknown) }
        }

        "delValue should delete a value on known key" {
          storage.delValue(known)
          storage.getValue(known) shouldBe null
        }

        "putValue should update a value on known key" {
          storage.putValue(known, "other".toByteArray())
          storage.getValue(known) shouldBe "other".toByteArray()
        }

        "putValue should add a value on unknown key" {
          storage.putValue(unknown, "other".toByteArray())
          storage.getValue(unknown) shouldBe "other".toByteArray()
        }
      },
  )
