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
import io.infinitic.tasks.TimeoutContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TaskRunner(
  private val executor: ExecutorService,
  private val logger: KLogger,
) {

  fun <T> runWithTimeout(
    name: String,
    timeoutMillis: Long,
    gracePeriodMillis: Long,
    task: () -> T
  ): Result<T> {
    val timeoutContext = TimeoutContext()

    val future = executor.submit<Result<T>> {
      timeoutContext.executingThread = Thread.currentThread().apply { this.name = "task-$name" }
      TimeoutContext.set(timeoutContext)

      try {
        Result.success(task())
      } catch (ex: Exception) {
        Result.failure(ex)
      } finally {
        TimeoutContext.clear()
      }
    }

    return try {
      // Try to get the result within the timeout
      future.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (timeoutEx: TimeoutException) {
      logger.warn {
        buildString {
          append("Task '$name' timed out after ${timeoutMillis}ms. ")
          if (gracePeriodMillis > 0) {
            append("Waiting for ${gracePeriodMillis}ms grace period to clean up...")
          } else {
            append("No grace period defined, cleaning up immediately.")
          }
        }
      }

      // Call the timeout callback if defined
      try {
        timeoutContext.onTimeOut()
      } catch (callbackEx: Exception) {
        logger.error(callbackEx) { "Task '$name' error during timeout callback execution" }
      }

      // Give user the opportunity to clean up during the grace period
      if (gracePeriodMillis > 0) {
        try {
          future.get(gracePeriodMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
          logger.warn { "Task '$name' still running after timeout + grace" }
        } catch (e: Exception) {
          logger.error(e) { "Task '$name' error during grace period after timeout" }
        }
      }

      // Still not done? Cancel and log
      if (!future.isDone) future.cancel(true)

      // Returns failure with the initial timeout exception
      Result.failure(timeoutEx)
    } catch (ex: ExecutionException) {
      Result.failure(ex.cause ?: ex)
    } catch (ex: Exception) {
      Result.failure(ex)
    }
  }
}
