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
package io.infinitic.storage.databases.redis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.resps.ScanResult

class RedisKeySetStoragePagingTests : FunSpec(
    {
      test("getPage should buffer scan overflow in the cursor") {
        val jedis = mockk<Jedis>(relaxed = true)
        val pool = mockk<JedisPool>()
        every { pool.resource } returns jedis
        every {
          jedis.sscan(
              any<ByteArray>(),
              ScanParams.SCAN_POINTER_START.toByteArray(),
              any(),
          )
        } returns ScanResult(
            "17",
            listOf("workflow-a".toByteArray(), "workflow-b".toByteArray(), "workflow-c".toByteArray()),
        )
        every {
          jedis.sscan(
              any<ByteArray>(),
              "17".toByteArray(),
              any(),
          )
        } returns ScanResult(
            ScanParams.SCAN_POINTER_START,
            listOf("workflow-d".toByteArray()),
        )

        val storage = RedisKeySetStorage(pool)

        val firstPage = storage.getPage("tag-key", limit = 2)

        firstPage.values.map(::String) shouldContainExactly listOf("workflow-a", "workflow-b")
        firstPage.nextCursor shouldBe
            "buffered:17:d29ya2Zsb3ctYw"

        val secondPage = storage.getPage("tag-key", limit = 2, cursor = firstPage.nextCursor)

        secondPage.values.map(::String) shouldContainExactly listOf("workflow-c", "workflow-d")
        secondPage.nextCursor shouldBe null
      }
    },
)
