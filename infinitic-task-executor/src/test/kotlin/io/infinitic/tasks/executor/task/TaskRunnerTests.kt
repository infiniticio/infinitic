/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
package io.infinitic.tasks.executor.task

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.TimeoutContext
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class TaskRunnerTests : StringSpec(
    {
      val taskId = TaskId()
      val serviceName = ServiceName("test-service")
      val taskName = MethodName("testTask")

      val taskContext = mockk<TaskContext>(relaxed = true) {
        every { this@mockk.serviceName } returns serviceName
        every { this@mockk.taskName } returns taskName
        every { this@mockk.taskId } returns taskId
      }

      val mockLogger = mockk<KLogger>(relaxed = true)
      val executor = Executors.newFixedThreadPool(4)
      val taskRunner = TaskRunner(executor, mockLogger)

      afterSpec {
        executor.shutdown()
      }

      "successfully run a task" {
        val result = taskRunner.runWithTimeout(taskContext, 1000, 100) {
          "success"
        }

        result.isSuccess shouldBe true
        result.getOrNull() shouldBe "success"
      }


      "handle task failure" {
        val exception = RuntimeException("Task failed")
        val result = taskRunner.runWithTimeout(taskContext, 1000, 100) {
          throw exception
        }

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe exception
      }

      "timeout task that runs too long" {
        val startTime = System.currentTimeMillis()
        val latch = CountDownLatch(1)

        val result = taskRunner.runWithTimeout(taskContext, 50, 10) {
          latch.countDown() // Signal that task has started
          Thread.sleep(200)
          "should not reach here"
        }

        // Wait for the task to start before checking duration
        latch.await(100, TimeUnit.MILLISECONDS)
        val duration = System.currentTimeMillis() - startTime

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<TimeoutException>()
        // Just verify it took at least the timeout period
        duration shouldBeGreaterThanOrEqual 50L
      }

      "execute timeout callback when task times out" {
        val latch = CountDownLatch(1)
        var callbackExecuted = false

        val task = {
          TimeoutContext.current().onTimeout {
            callbackExecuted = true
            latch.countDown()
          }
          Thread.sleep(200)
          "should not reach here"
        }

        val result = taskRunner.runWithTimeout(taskContext, 50, 10, task)

        // Wait for the callback to be executed
        latch.await(100, TimeUnit.MILLISECONDS)

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<TimeoutException>()
        callbackExecuted shouldBe true
      }

      "handle task that completes during grace period" {
        val latch = CountDownLatch(1)
        var completed = false

        val task = {
          Thread.sleep(30) // Shorter sleep to ensure it completes during grace period
          completed = true
          latch.countDown()
          "completed in grace period"
        }

        val result = taskRunner.runWithTimeout(taskContext, 10, 100, task)

        // Wait for the task to complete
        latch.await(200, TimeUnit.MILLISECONDS)

        completed shouldBe true
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe "completed in grace period"
      }

      "handle task that throws Error" {
        val error = OutOfMemoryError("Simulated error")
        val result = taskRunner.runWithTimeout(taskContext, 1000, 100) {
          throw error
        }

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
      }

      "set and clear thread name during execution" {
        var threadName: String? = null

        taskRunner.runWithTimeout(taskContext, 1000, 100) {
          threadName = Thread.currentThread().name
          "success"
        }

        threadName shouldBe "task-${serviceName}:${taskName}-${taskId}"
      }

      "handle task cancellation during execution" {
        val result = taskRunner.runWithTimeout(taskContext, 50, 0) {
          try {
            Thread.sleep(200)
            "should not reach here"
          } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
          }
        }

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<TimeoutException>()
      }

      "log warning when task times out" {
        var logMessage = ""
        every { mockLogger.warn(captureLambda()) } answers {
          logMessage = firstArg()
        }

        taskRunner.runWithTimeout(taskContext, 50, 10) {
          Thread.sleep(100)
          "should not reach here"
        }

        verify { mockLogger.warn(any<() -> String>()) }
        logMessage shouldContain "timed out after 50ms"
        logMessage shouldContain "grace period"
      }

      "log error when task exceeds grace period" {
        var logMessage = ""
        every { mockLogger.error(captureLambda()) } answers {
          logMessage = firstArg()
        }

        taskRunner.runWithTimeout(taskContext, 10, 10) {
          Thread.sleep(100)
          "should not reach here"
        }

        verify { mockLogger.error(any<() -> String>()) }
        logMessage shouldContain "still running after timeout + grace"
      }
    },
)
