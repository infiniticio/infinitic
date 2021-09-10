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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import redis.embedded.RedisServer

class RedisKeyCounterStorageTests : StringSpec({

    val redisServer = RedisServer(6380)
    val storage = RedisKeyCounterStorage.of(Redis("localhost", 6380))

    beforeSpec {
        redisServer.start()
    }

    afterSpec {
        redisServer.stop()
    }

    beforeTest {
        storage.incr("foo", 42)
    }

    afterTest {
        storage.flush()
    }

    "getCounter should return 0 on unknown key" {
        storage.get("unknown") shouldBe 0
    }

    "getCounter should return value on known key" {
        storage.get("foo") shouldBe 42
    }

    "incrCounter on unknown key should incr value from 0" {
        storage.incr("unknown", 42)

        storage.get("unknown") shouldBe 42
    }

    "incrCounter on known key should incr value from current" {
        storage.incr("foo", -7)

        storage.get("foo") shouldBe 35
    }
})
