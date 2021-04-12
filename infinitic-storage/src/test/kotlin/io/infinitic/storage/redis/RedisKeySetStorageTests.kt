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

package io.infinitic.storage.redis

import io.infinitic.common.data.Bytes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import redis.embedded.RedisServer

class RedisKeySetStorageTests : StringSpec({

    val redisServer = RedisServer(6380)
    val storage = RedisKeySetStorage.of(Redis("localhost", 6380))

    beforeSpec {
        redisServer.start()
    }

    afterSpec {
        redisServer.stop()
    }

    beforeTest {
        storage.addToSet("foo", "bar".toByteArray())
    }

    afterTest {
        storage.flush()
    }

    fun equalsTo(set1: Set<ByteArray>, set2: Set<ByteArray>) =
        set1.map { Bytes(it) }.toSet() == set2.map { Bytes(it) }.toSet()

    "getSet should return an empty set on unknown key" {
        storage.getSet("unknown") shouldBe setOf<ByteArray>()
    }

    "getSet should return the set on known key" {
        equalsTo(storage.getSet("foo"), setOf("bar".toByteArray())) shouldBe true
    }

    "addToSet on unknown key should create a new set" {
        storage.addToSet("unknown", "42".toByteArray())

        equalsTo(storage.getSet("unknown"), setOf("42".toByteArray())) shouldBe true
    }

    "addToSet on known key should add to set" {
        storage.addToSet("foo", "42".toByteArray())

        equalsTo(storage.getSet("foo"), setOf("42".toByteArray(), "bar".toByteArray())) shouldBe true
    }

    "addToSet known value on known key should do nothing" {
        storage.addToSet("foo", "bar".toByteArray())

        equalsTo(storage.getSet("foo"), setOf("bar".toByteArray())) shouldBe true
    }

    "removeFromSet on unknown key should do nothing" {
        storage.removeFromSet("unknown", "42".toByteArray())
    }

    "removeFromSet unknown value on known key should do nothing" {
        storage.removeFromSet("foo", "42".toByteArray())

        equalsTo(storage.getSet("foo"), setOf("bar".toByteArray())) shouldBe true
    }

    "removeFromSet known value on known key should remove from set" {
        storage.removeFromSet("foo", "bar".toByteArray())

        equalsTo(storage.getSet("foo"), setOf()) shouldBe true
    }
})
