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
package io.infinitic.common.transport.consumers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import kotlin.math.abs
import kotlin.random.Random

fun main() {
  repeat(100) {
    println(getShard(it.toString(), 2))
  }
}

class GetShardTest : StringSpec(
    {

      "should return 0 for single shard" {
        getShard(RandomStringUtils.random(10), 1) shouldBe 0
      }

      "shard should be in range and regularly distributed" {
        repeat(10) {
          val shards = Random.nextInt(1, 20)
          val results = mutableMapOf<Int, Int>()
          repeat(10000) {
            val shard = getShard(RandomStringUtils.random(10), shards)
            results[shard] = results.getOrDefault(shard, 0) + 1
          }
          results.keys shouldBe (0 until shards).toSet()
          results.values.forEach { abs(it - (10000.0 / shards)) shouldBeLessThan 1500.0 / shards }
        }
      }

      "should consistently return the same shard for the same input and shard count" {
        repeat(100) {
          val input = RandomStringUtils.random(10)
          val shards = Random.nextInt(1, 20)
          val expectedShard = getShard(input, shards)
          coroutineScope {
            repeat(100) {
              launch { getShard(input, shards) shouldBe expectedShard }
            }
          }
        }
      }
    },
)
