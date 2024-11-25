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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.fixtures.later
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StartReceivingAndShardTest : StringSpec(
    {

      val logger = LoggerWithCounter(KotlinLogging.logger("io.infinitic.tests"))

      fun getScope() = CoroutineScope(Dispatchers.IO)

      fun areListsDisjoint(map: Map<Int, Set<String>>): Boolean {
        val seenElements = mutableSetOf<String>()
        for (list in map.values) {
          for (element in list) {
            if (!seenElements.add(element)) {
              return false // Element already seen, lists are not disjoint
            }
          }
        }
        return true // All elements are unique across all lists
      }

      "Consumer should consume up to scope cancellation" {
        with(logger) {
          val scope = getScope()
          val concurrency = 5
          val channels = scope.startReceivingAndShard(IntConsumer(), true, concurrency)

          channels.size shouldBe concurrency

          later(200) { scope.cancel() }

          val mutex = Mutex()
          val keyMap = mutableMapOf<Int, MutableSet<String>>()

          scope.launch {
            channels.forEachIndexed { index, channel ->
              launch {
                while (isActive) {
                  val i = channel.receive().message
                  mutex.withLock {
                    keyMap.getOrPut(index) { mutableSetOf() }.add(i.key)
                  }
                }
              }
            }
          }.join()

          // Checking that all channels do not share keys
          areListsDisjoint(keyMap) shouldBe true

          // Note: before triggering the exception, the channel will serve previously sent messages
          channels.forEach { channel ->
            shouldThrow<ClosedReceiveChannelException> { while (true) channel.receive() }
          }
        }
      }
    },
)
