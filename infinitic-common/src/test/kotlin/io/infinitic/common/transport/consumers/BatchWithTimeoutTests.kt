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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

@ExperimentalCoroutinesApi
class BatchWithTimeoutTests : StringSpec(
    {

      "should batch messages until maxMessages is reached" {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val numMessages = 5
        val maxMessages = 3
        val maxMillis = 1000L

        (1..numMessages).forEach { channel.send(it) }
        channel.close()

        val result = channel.batchWithTimeout(maxMessages, maxMillis)

        result.messages shouldContainExactly listOf(1, 2, 3)
        result.isChannelOpen shouldBe true
      }

      "should batch messages until channel is closed" {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val numMessages = 5
        val maxMessages = 6
        val maxMillis = 1000L

        (1..numMessages).forEach { channel.send(it) }
        channel.close()

        val result = channel.batchWithTimeout(maxMessages, maxMillis)

        result.messages shouldContainExactly listOf(1, 2, 3, 4, 5)
        result.isChannelOpen shouldBe false
      }

      "should return messages until timeout occurs" {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val maxMessages = 5
        val maxMillis = 500L

        channel.send(1)
        channel.send(2)

        val result = channel.batchWithTimeout(maxMessages, maxMillis)

        result.messages shouldContainExactly listOf(1, 2)
        result.isChannelOpen shouldBe true
        channel.close()
      }

      "should return an empty batch if timeout occurs before any messages are received" {
        val channel = Channel<Int>(Channel.UNLIMITED)
        val maxMessages = 5
        val maxMillis = 10L

        val result = channel.batchWithTimeout(maxMessages, maxMillis)

        result.messages shouldBe emptyList()
        result.isChannelOpen shouldBe true
        channel.close()
      }

      "should consider the isOpen flag correctly if the channel is closed" {
        runTest {
          val channel = Channel<Int>(Channel.UNLIMITED)
          val maxMessages = 5
          val maxMillis = 1000L

          channel.send(1)
          channel.close()

          val result = channel.batchWithTimeout(maxMessages, maxMillis)

          result.messages shouldContainExactly listOf(1)
          result.isChannelOpen shouldBe false
        }
      }
    },
)
