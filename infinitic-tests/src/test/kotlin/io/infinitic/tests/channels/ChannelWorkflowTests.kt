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

import io.infinitic.clients.InfiniticClient
import io.infinitic.common.fixtures.later
import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.FailedWorkflowTaskException
import io.infinitic.exceptions.workflows.OutOfBoundAwaitException
import io.infinitic.tests.utils.Obj1
import io.infinitic.tests.utils.Obj2
import io.infinitic.workers.InfiniticWorker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.coroutines.delay

internal class ChannelWorkflowTests :
    StringSpec({

      // each test should not be longer than 10s
      timeout = 10000

      val worker = autoClose(InfiniticWorker.fromConfigResource("/pulsar.yml"))
      val client = autoClose(InfiniticClient.fromConfigResource("/pulsar.yml"))

      val channelsWorkflow =
          client.newWorkflow(ChannelsWorkflow::class.java, tags = setOf("foo", "bar"))

      beforeSpec { worker.startAsync() }

      beforeTest { worker.registry.flush() }

      "Waiting for event, sent after dispatched" {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent by id" {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent by tag" {
        val deferred = client.dispatch(channelsWorkflow::channel1)

        later { client.getWorkflowByTag(ChannelsWorkflow::class.java, "foo").channelA.send("test") }

        deferred.await() shouldBe "test"
      }

      "Waiting for event, sent to the right channel" {
        val deferred = client.dispatch(channelsWorkflow::channel2)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelA.send("test")
        }

        deferred.await() shouldBe "test"
      }

      "Waiting for event but sent to the wrong channel" {
        val deferred = client.dispatch(channelsWorkflow::channel2)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelB.send("test")
        }

        deferred.await()::class.java.name shouldBe Instant::class.java.name
      }

      "Sending event before waiting for it prevents catching" {
        val deferred = client.dispatch(channelsWorkflow::channel3)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelA.send("test")
        }

        deferred.await()::class.java.name shouldBe Instant::class.java.name
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
          w.channelA.send("a")
          w.channelA.send("b")
          w.channelA.send("c")
        }

        deferred.await() shouldBe "abc"
      }

      "Waiting for multiple events on same deferred, client being late" {
        val deferred = client.dispatch(channelsWorkflow::channel7, 3, 3)

        later {
          val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
          delay(300)
          w.channelA.send("a")
          delay(300)
          w.channelA.send("b")
          delay(300)
          w.channelA.send("c")
        }

        deferred.await() shouldBe "abc"
      }

      "Waiting for a lot of events events on same deferred" {
        val count = 20
        val deferred = client.dispatch(channelsWorkflow::channel7, count)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later { repeat(count) { w.channelA.send("a") } }

        deferred.await() shouldBe "a".repeat(count)
      }

      "Waiting for more signals than anticipated throw a OutOfBoundAwaitException" {
        val count = 2
        val deferred = client.dispatch(channelsWorkflow::channel7, count, 1)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later { repeat(count) { w.channelA.send("a") } }

        val error = shouldThrow<FailedWorkflowException> { deferred.await() }

        (error.deferredException as FailedWorkflowTaskException).workerException.name shouldBe
            OutOfBoundAwaitException::class.java.name
      }

      "Waiting for more signals than anticipated throw a OutOfBoundAwaitException, client being late" {
        val count = 2
        val deferred = client.dispatch(channelsWorkflow::channel7, count, 1)

        val w = client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id)
        later {
          repeat(count) {
            delay(300)
            w.channelA.send("a")
          }
        }

        val error = shouldThrow<FailedWorkflowException> { deferred.await() }

        (error.deferredException as FailedWorkflowTaskException).workerException.name shouldBe
            OutOfBoundAwaitException::class.java.name
      }

      "testing isCompleted" {
        val deferred = client.dispatch(channelsWorkflow::channel8)

        later {
          client.getWorkflowById(ChannelsWorkflow::class.java, deferred.id).channelA.send("test")
        }

        deferred.await() shouldBe "falsetruefalse"
      }
    })
