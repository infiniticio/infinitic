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
package io.infinitic.tests.channels

import io.infinitic.Test
import io.infinitic.common.fixtures.later
import io.infinitic.utils.Obj1
import io.infinitic.utils.Obj2
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

internal class ChannelWorkflowTests : StringSpec(
    {
      val client = Test.client

      val channelsWorkflow =
          client.newWorkflow(ChannelsWorkflow::class.java, tags = setOf("foo", "bar"))

      "Waiting for event, sent after dispatched".config(timeout = 30.seconds) {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later {
          client.getWorkflowById(
              ChannelsWorkflow::class.java,
              deferred.id,
          ).channelStrA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent by id" {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later {
          client.getWorkflowById(
              ChannelsWorkflow::class.java,
              deferred.id,
          ).channelStrA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent by tag" {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later {
          client.getWorkflowByTag(
              ChannelsWorkflow::class.java,
              "foo",
          ).channelStrA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent to the right channel" {
        val deferred = client.dispatch(channelsWorkflow::channel2)

        client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
            .channelStrA.send("test")

        deferred.await() shouldBe "test"
      }

      "Waiting for event but sent to the wrong channel" {
        val deferred = client.dispatch(channelsWorkflow::channel2)

        client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
            .channelStrB.send("test")

        deferred.await() shouldBe "Instant"
      }

      "Event is discarded if sent before waiting for it" {
        val deferred = client.dispatch(channelsWorkflow::channel3)

        later(10) {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
              .channelStrA.send("test")
        }

        deferred.await() shouldBe "Instant"
      }

      "Waiting for Obj event" {
        val obj1 = Obj1("foo", 42)
        val deferred = client.dispatch(channelsWorkflow::channel4)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
      }

      "Waiting for filtered event using jsonPath only" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.dispatch(channelsWorkflow::channel4bis)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj1a)
          w.channelObj.send(obj1b)
        }

        deferred.await() shouldBe obj1b
      }

      "Waiting for filtered event using using jsonPath and criteria" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.dispatch(channelsWorkflow::channel4ter)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj1a)
          w.channelObj.send(obj1b)
        }

        deferred.await() shouldBe obj1b
      }

      "Waiting for event of specific type" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val deferred = client.dispatch(channelsWorkflow::channel5)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj2)
          w.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
      }

      "Waiting event of specific type filtered using jsonPath only" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.dispatch(channelsWorkflow::channel5bis)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj3)
          w.channelObj.send(obj2)
          w.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
      }

      "Waiting event of specific type filtered using jsonPath and criteria" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.dispatch(channelsWorkflow::channel5ter)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj3)
          w.channelObj.send(obj2)
          w.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
      }

      "Waiting for 2 events of specific types presented in wrong order" {
        val obj1 = Obj1("foo", 6)
        val obj2 = Obj2("bar", 7)
        val deferred = client.dispatch(channelsWorkflow::channel6)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj2)
          w.channelObj.send(obj1)
        }

        deferred.await() shouldBe "foobar42"
      }

      "Waiting for 2 events of specific types filtered using jsonPath and criteria presented in wrong order" {
        val obj1 = Obj1("foo", 5)
        val obj2 = Obj2("bar", 6)
        val obj3 = Obj2("foo", 7)
        val deferred = client.dispatch(channelsWorkflow::channel6bis)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelObj.send(obj2)
          w.channelObj.send(obj1)
          w.channelObj.send(obj3)
        }

        deferred.await() shouldBe "foofoo35"
      }

      "Waiting for multiple events on same deferred" {
        val deferred = client.dispatch(channelsWorkflow::channel7, 3)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          w.channelStrA.send("a")
          w.channelStrA.send("b")
          w.channelStrA.send("c")
        }

        deferred.await() shouldBe "abc"
      }

      "Waiting for multiple events on same deferred, client being late" {
        val deferred = client.dispatch(channelsWorkflow::channel7, 3, 3)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          delay(300)
          w.channelStrA.send("a")
          delay(300)
          w.channelStrA.send("b")
          delay(300)
          w.channelStrA.send("c")
        }

        deferred.await() shouldBe "abc"
      }

      "Waiting for a lot of events events on same deferred" {
        val count = 20
        val deferred = client.dispatch(channelsWorkflow::channel7, count)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later { repeat(count) { w.channelStrA.send("a") } }

        deferred.await() shouldBe "a".repeat(count)
      }

      "Waiting for more signals than anticipated should not throw and repeat last value" {
        val deferred = client.dispatch(channelsWorkflow::channel7, 3, 2)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later {
          w.channelStrA.send("a")
          w.channelStrA.send("b")
          w.channelStrA.send("c")
        }

        shouldNotThrowAny { deferred.await() } shouldBe "abb"
      }

      "Waiting for more signals than anticipated should not throw and repeat last value, client being late" {
        val deferred = client.dispatch(channelsWorkflow::channel7, 3, 2)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later {
          delay(100)
          w.channelStrA.send("a")
          delay(100)
          w.channelStrA.send("b")
          delay(100)
          w.channelStrA.send("c")
        }

        shouldNotThrowAny { deferred.await() } shouldBe "abb"
      }

      "testing isCompleted" {
        val deferred = client.dispatch(channelsWorkflow::channel8)

        later(0) {
          client.getWorkflowById(
              ChannelsWorkflow::class.java,
              deferred.id,
          ).channelStrA.send("test")
        }

        deferred.await() shouldBe "falsetruefalse"
      }

      "testing multiple incomplete await on a channel" {
        channelsWorkflow.channel9() shouldBe "ok"
      }
    },
)
