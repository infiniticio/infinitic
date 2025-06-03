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
package io.infinitic.tests.batches

import io.infinitic.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class BatchesWorkflowTests : StringSpec(
    {
      val client = Test.client

      val batchWorkflow = client.newWorkflow(BatchWorkflow::class.java)

      "initialization".config(timeout = 1.minutes) {
        delay(20000)
      }

      "One primitive parameter (with maxMessages=10)" {
        BatchServiceImpl.foos.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo, i)
        }
        batchWorkflow.foo(30)

        println("BatchServiceImpl.foos.average: ${BatchServiceImpl.foos.average()}")
        BatchServiceImpl.foos.average() shouldBeGreaterThan 2.0
      }

      "One primitive parameter (with maxSeconds=0.25)" {
        BatchServiceImpl.foos.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo, i)
          delay(30)
        }
        batchWorkflow.foo(30)
        println("BatchServiceImpl.foos.average: ${BatchServiceImpl.foos.average()}")
        BatchServiceImpl.foos.average() shouldBeGreaterThan 2.0
      }

      "One Object parameter (with maxMessages=10)" {
        BatchServiceImpl.foo2s.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo2, i, i)
        }
        batchWorkflow.foo2(30, 30)
        println("BatchServiceImpl.foo2s.average: ${BatchServiceImpl.foo2s.average()}")
        BatchServiceImpl.foo2s.average() shouldBeGreaterThan 2.0
      }

      "One Object parameter (with maxSeconds=0.25)" {
        BatchServiceImpl.foo2s.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo2, i, i)
          delay(30)
        }
        batchWorkflow.foo2(30, 30)
        println("BatchServiceImpl.foo2s.average: ${BatchServiceImpl.foo2s.average()}")
        BatchServiceImpl.foo2s.average() shouldBeGreaterThan 2.0
      }

      "Returns Object (with maxMessages=10)" {
        BatchServiceImpl.foo4s.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo4, i, i)
        }
        batchWorkflow.foo4(30, 30)

        println("BatchServiceImpl.foo4s.average: ${BatchServiceImpl.foo4s.average()}")
        BatchServiceImpl.foo4s.average() shouldBeGreaterThan 2.0
      }

      "Returns Object (with maxSeconds=0.25)" {
        BatchServiceImpl.foo4s.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo4, i, i)
          delay(30)
        }
        batchWorkflow.foo4(30, 30)

        println("BatchServiceImpl.foo4s.average: ${BatchServiceImpl.foo4s.average()}")
        BatchServiceImpl.foo4s.average() shouldBeGreaterThan 2.0
      }

      "One Object parameter and returns Object(with maxMessages=10)" {
        BatchServiceImpl.foo5s.clear()
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo5, i, i)
        }
        batchWorkflow.foo5(30, 30)

        println("BatchServiceImpl.foo5s.average: ${BatchServiceImpl.foo5s.average()}")
        BatchServiceImpl.foo5s.average() shouldBeGreaterThan 2.0
      }

      "One Object parameter and returns Object (with maxSeconds=0.25)" {
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo5, i, i)
          delay(30)
        }
        batchWorkflow.foo5(30, 30)

        println("BatchServiceImpl.foo5s.average: ${BatchServiceImpl.foo5s.average()}")
        BatchServiceImpl.foo5s.average() shouldBeGreaterThan 2.0
      }


      "No return Object(with maxMessages=10)" {
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo6, i, i)
        }
        batchWorkflow.foo6(30, 30)

        println("BatchServiceImpl.foo6s.average: ${BatchServiceImpl.foo6s.average()}")
        BatchServiceImpl.foo6s.average() shouldBeGreaterThan 2.0
      }

      "No return Object (with maxSeconds=0.25)" {
        for (i in 1..29) {
          client.dispatch(batchWorkflow::foo6, i, i)
          delay(30)
        }
        batchWorkflow.foo6(30, 30) shouldBe Unit

        println("BatchServiceImpl.foo6s.average: ${BatchServiceImpl.foo6s.average()}")
        BatchServiceImpl.foo6s.average() shouldBeGreaterThan 2.0
      }

      "If Task contains a batch key, all batches should have the same key" {
        batchWorkflow.withKey(20) shouldBe true
      }

      "checking workflow executor batching" {
        // then we measure
        val scope = CoroutineScope(Dispatchers.IO)
        val duration = measureTimeMillis {
          scope.launch {
            coroutineScope {
              repeat(20) {
                launch { println("starting $it"); batchWorkflow.withDelay(1000); println("ending $it"); }
              }
            }
          }.join()
        }
        println("duration: $duration")
        duration shouldBeLessThan 10000L
      }

      "test 1" {
        // we execute a first workflow to initialize
        batchWorkflow.withDelay(1)
        // then we measure
        val scope = CoroutineScope(Dispatchers.IO)
        val duration = measureTimeMillis {
          scope.launch {
            coroutineScope {
              launch { batchWorkflow.withDelay(1000) }
            }
          }.join()
        }
        println("duration: $duration")
      }
    },
)
