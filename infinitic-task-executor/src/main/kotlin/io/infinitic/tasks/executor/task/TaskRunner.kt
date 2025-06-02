package io.infinitic.tasks.executor.task

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.TimeoutContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Runs a task with a specified timeout and optional grace period.
 *
 * @property executor The `ExecutorService` used to run the task.
 * @property logger The logger for logging task events.
 */
class TaskRunner(
  private val executor: ExecutorService,
  private val logger: KLogger,
) {

  /**
   * Executes a task with a timeout and an optional grace period.
   *
   * @param taskContext The context of the task being executed.
   * @param timeoutMillis The maximum time in milliseconds to allow the task to run before timing out.
   * @param graceMillis The additional grace period in milliseconds to wait after a timeout before forcefully cancelling the task.
   * @param task The task to execute.
   * @return A [Result] containing the task's output or an exception if it failed or timed out.
   */
  fun <T> runWithTimeout(
    taskContext: TaskContext,
    timeoutMillis: Long,
    graceMillis: Long,
    task: () -> T
  ): Result<T> {
    val name = "${taskContext.serviceName}:${taskContext.taskName}-${taskContext.taskId}"
    val timeoutContext = TimeoutContext()
    //
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

    var output: Result<T>

    try {
      output = future.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (timeoutEx: TimeoutException) {
      logger.warn { "Task '$name' timed out after ${timeoutMillis}ms. Waiting for ${graceMillis}ms grace period..." }

      try {
        // set Task.hasTimedOut to true and invoke the custom timeout callback, if any
        timeoutContext.cancel()
      } catch (callbackEx: Exception) {
        logger.error(callbackEx) { "Task '$name' error during timeout callback execution" }
      }

      // Grace period
      if (!future.isDone) {
        try {
          future.get(graceMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
          logger.warn { "Task '$name' exceeded grace period of $graceMillis ms" }
        }
      }

      // Still not done after grace?
      if (!future.isDone) {
        if (timeoutContext.executingThread?.isAlive == true) {
          logger.error { "Task '$name' still running after timeout + grace" }
        } else {
          logger.warn { "Task '$name' not done, but thread not alive — possible race or shutdown" }
        }
        // Cancel the task
        future.cancel(true)
      }

      // Try to get a result anyway (in case it completed during grace)
      if (future.isDone) {
        try {
          output = future.get()
        } catch (ex: Exception) {
          output = Result.failure(ex)
        }
      } else {
        output = Result.failure(timeoutEx)
      }
    } catch (ex: Exception) {
      output = Result.failure(ex)
    }

    return output
  }
}
