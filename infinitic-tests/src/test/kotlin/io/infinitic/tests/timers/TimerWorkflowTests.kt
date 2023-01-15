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
package io.infinitic.tests.timers

import io.infinitic.clients.InfiniticClient
import io.infinitic.common.fixtures.later
import io.infinitic.workers.InfiniticWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.Instant

internal class TimerWorkflowTests :
    StringSpec({

      // each test should not be longer than 10s
      timeout = 10000

      val worker = autoClose(InfiniticWorker.fromConfigResource("/pulsar.yml"))
      val client = autoClose(InfiniticClient.fromConfigResource("/pulsar.yml"))

      val timerWorkflow = client.newWorkflow(TimerWorkflow::class.java, tags = setOf("foo", "bar"))

      beforeSpec { worker.startAsync() }

      beforeTest { worker.registry.flush() }

      "Wait for a duration timer" {
        val start = Instant.now().toEpochMilli()

        val deferred = client.dispatch(timerWorkflow::await, 200L)

        (deferred.await().toEpochMilli() - start) shouldBeLessThan (2000L)
      }

      "Wait for a long duration timer" {
        val start = Instant.now().toEpochMilli()

        val deferred = client.dispatch(timerWorkflow::await, 2000L)

        (deferred.await().toEpochMilli() - start) shouldBeGreaterThan (2000L)
      }

      "Wait for a instant timer" {
        val start = Instant.now().toEpochMilli()

        val deferred = client.dispatch(timerWorkflow::await, Instant.now().plusMillis(200))

        (deferred.await().toEpochMilli() - start) shouldBeLessThan (2000L)
      }

      "Wait for a long instant timer" {
        val start = Instant.now().toEpochMilli()

        val deferred = client.dispatch(timerWorkflow::await, Instant.now().plusMillis(2000))

        (deferred.await().toEpochMilli() - start) shouldBeGreaterThan (2000L)
      }

      "Wait for a timer or a signal - timer wins" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 200L)

        deferred.await()::class shouldBe Instant::class
      }

      "Wait for a timer or a signal - signal wins" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 10000L)

        later {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          w.channel.send("bingo")
        }

        deferred.await() shouldBe "bingo"
      }

      "Wait for a timer or a signal - timer wins after manual completion by id" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 10000L)

        later(200) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          client.completeTimers(w)
        }

        later(500) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          w.channel.send("bingo")
        }

        deferred.await()::class shouldBe Instant::class
      }

      "Wait for a timer or a signal - timer wins after manual completion by tag" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 10000L)

        later(200) {
          val w = client.getWorkflowByTag(TimerWorkflow::class.java, "foo")
          client.completeTimers(w)
        }

        later(500) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          w.channel.send("bingo")
        }

        deferred.await()::class shouldBe Instant::class
      }

      "Wait for a timer or a signal - signal wins after manual timer completion with wrong methodRunId" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 10000L)

        later(200) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          client.completeTimers(w, "wrong")
        }

        later(500) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          w.channel.send("bingo")
        }

        deferred.await() shouldBe "bingo"
      }

      "Wait for a timer or a signal - timer wins after manual completion with correct methodRunId" {
        val deferred = client.dispatch(timerWorkflow::awaitSignal, 10000L)

        later(200) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          client.completeTimers(w, deferred.id)
        }

        later(500) {
          val w = client.getWorkflowById(TimerWorkflow::class.java, deferred.id)
          w.channel.send("bingo")
        }

        deferred.await()::class shouldBe Instant::class
      }
    })
