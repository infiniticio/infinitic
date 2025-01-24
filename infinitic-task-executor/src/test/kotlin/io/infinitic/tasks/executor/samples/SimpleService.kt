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
@file:Suppress("unused")

package io.infinitic.tasks.executor.samples

import io.infinitic.annotations.Name
import io.infinitic.annotations.Retry
import io.infinitic.annotations.Timeout
import io.infinitic.tasks.Task
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout

@Name("SimpleService")
interface ServiceImplServiceI {
  fun handle(i: Int, j: String): String

  fun handle(i: Int, j: Int): Int

  fun other(i: Int, j: String): String

  fun withThrowable(): String
}

class ServiceImplService : ServiceImplServiceI {
  override fun handle(i: Int, j: String) = (i * j.toInt()).toString()

  override fun handle(i: Int, j: Int) = (i * j)

  override fun other(i: Int, j: String) = (i * j.toInt()).toString()

  override fun withThrowable(): String = throw Throwable("test throwable")
}

@Name("SimpleService")
internal interface SimpleService {
  fun handle(i: Int, j: String): String
}

internal class ServiceWithContext : SimpleService {
  override fun handle(i: Int, j: String) = (i * j.toInt() * Task.retrySequence).toString()
}

internal class SimpleServiceWithRetry : SimpleService, WithRetry {
  companion object {
    const val DELAY = 3.0
  }

  override fun handle(i: Int, j: String): String =
      if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

  override fun getSecondsBeforeRetry(retry: Int, e: Exception) = DELAY
}

@Retry(RetryImpl::class)
internal class ServiceWithRetryInClass : SimpleService {
  override fun handle(i: Int, j: String): String =
      if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
}

@Retry(BuggyRetryImpl::class)
internal class ServiceWithBuggyRetryInClass : SimpleService {
  override fun handle(i: Int, j: String): String =
      if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
}

internal class ServiceWithRetryInMethod : SimpleService {
  @Retry(RetryImpl::class)
  override fun handle(i: Int, j: String): String =
      if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()
}

internal class ServiceWithTimeout : SimpleService, WithTimeout {
  companion object {
    const val TIMEOUT = 0.1
  }

  override fun handle(i: Int, j: String): String {
    Thread.sleep(400)

    return (i * j.toInt() * Task.retrySequence).toString()
  }

  override fun getTimeoutSeconds(): Double = TIMEOUT
}

@Timeout(TimeoutImpl::class)
internal class ServiceWithTimeoutOnClass : SimpleService {
  override fun handle(i: Int, j: String): String {
    Thread.sleep(400)

    return (i * j.toInt() * Task.retrySequence).toString()
  }
}

internal class ServiceWithTimeoutOnMethod : SimpleService {
  @Timeout(TimeoutImpl::class)
  override fun handle(i: Int, j: String): String {
    Thread.sleep(400)

    return (i * j.toInt() * Task.retrySequence).toString()
  }
}

internal class ServiceWithRegisteredTimeout : SimpleService {
  override fun handle(i: Int, j: String): String {
    Thread.sleep(400)

    return (i * j.toInt() * Task.retrySequence).toString()
  }
}

internal class RetryImpl : WithRetry {
  companion object {
    const val DELAY = 3.0
  }

  override fun getSecondsBeforeRetry(retry: Int, e: Exception) =
      if (e is IllegalStateException) DELAY else null
}

internal class BuggyRetryImpl : WithRetry {
  companion object {
    const val DELAY = 3.0
  }

  override fun getSecondsBeforeRetry(retry: Int, e: Exception) =
      if (e is IllegalStateException) throw IllegalArgumentException() else DELAY
}

internal class TimeoutImpl : WithTimeout {
  companion object {
    const val TIMEOUT = 0.1
  }

  override fun getTimeoutSeconds() = TIMEOUT
}

internal class BuggyTimeoutImpl : WithTimeout {
  override fun getTimeoutSeconds(): Double = throw IllegalArgumentException()
}
