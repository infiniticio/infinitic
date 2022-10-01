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

package io.infinitic.cache.caffeine

import io.infinitic.cache.config.caffeine.Caffeine
import io.infinitic.cache.config.caffeine.CaffeineCachedKeySet
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CaffeineKeySetCacheTests : StringSpec({

    val storage = CaffeineCachedKeySet(Caffeine())

    beforeTest {
        storage.set("key", setOf("foo".toByteArray(), "bar".toByteArray()))
    }

    afterTest {
        storage.flush()
    }

    "get should return null on unknown key" {
        storage.get("unknown") shouldBe null
    }

    "get should return value" {
        val set = storage.get("key")!!
        set.map { String(it) }.toSet() shouldBe setOf("foo", "bar")
    }

    "add on new key should stay null" {
        storage.add("foo2", "bar2".toByteArray())

        storage.get("foo2") shouldBe null
    }

    "add on existing key should update value" {
        storage.add("key", "bar2".toByteArray())

        storage.get("key")!!.map { String(it) }.toSet() shouldBe setOf("foo", "bar", "bar2")
    }

    "remove on unknown key does nothing" {
        storage.remove("unknown", "foo".toByteArray())
    }

    "remove should remove value" {
        storage.remove("key", "foo".toByteArray())

        storage.get("key")!!.map { String(it) }.toSet() shouldBe setOf("bar")
    }

    "remove should do nothing if not on set" {
        storage.remove("key", "unknown".toByteArray())

        storage.get("key")!!.map { String(it) }.toSet() shouldBe setOf("foo", "bar")
    }
})
