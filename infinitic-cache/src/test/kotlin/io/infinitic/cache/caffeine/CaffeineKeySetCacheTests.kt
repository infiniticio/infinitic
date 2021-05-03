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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CaffeineKeySetCacheTests : StringSpec({

    val storage = CaffeineKeySetCache(Caffeine())

    beforeTest {
        storage.setSet("key", setOf("foo".toByteArray(), "bar".toByteArray()))
    }

    afterTest {
        storage.flush()
    }

    "getSet should return null on unknown key" {
        storage.getSet("unknown") shouldBe null
    }

    "getSet should return value" {
        val set = storage.getSet("key")!!
        set.map { String(it) }.toSet() shouldBe setOf("foo", "bar")
    }

    "addToSet on new key should stay null" {
        storage.addToSet("foo2", "bar2".toByteArray())

        storage.getSet("foo2") shouldBe null
    }

    "addToSet on existing key should update value" {
        storage.addToSet("key", "bar2".toByteArray())

        storage.getSet("key")!!.map { String(it) }.toSet() shouldBe setOf("foo", "bar", "bar2")
    }

    "removeFromSet on unknown key does nothing" {
        storage.removeFromSet("unknown", "foo".toByteArray())
    }

    "removeFromSet should remove value" {
        storage.removeFromSet("key", "foo".toByteArray())

        storage.getSet("key")!!.map { String(it) }.toSet() shouldBe setOf("bar")
    }

    "removeFromSet should do nothing if not on set" {
        storage.removeFromSet("key", "unknown".toByteArray())

        storage.getSet("key")!!.map { String(it) }.toSet() shouldBe setOf("foo", "bar")
    }
})
