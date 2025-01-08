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
package io.infinitic.exceptions

import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.workers.data.WorkerName

data class WorkerException(
  /** Name of the worker */
  val workerName: String,

  /** Name of the error */
  val name: String,

  /** Message of the error */
  override val message: String?,

  /** String version of the stack trace */
  val stackTraceToString: String,

  /** cause of the error */
  override val cause: WorkerException?
) : RuntimeException() {
  companion object {
    fun from(error: ExecutionError): WorkerException =
        WorkerException(
            workerName = error.workerName.toString(),
            name = error.name,
            message = error.message,
            stackTraceToString = error.stackTraceToString,
            cause = error.cause?.let { from(it) },
        )

    fun from(workerName: WorkerName, throwable: Throwable): WorkerException =
        WorkerException(
            workerName = workerName.toString(),
            name = throwable::class.java.name,
            message = throwable.message,
            stackTraceToString = throwable.stackTraceToString(),
            cause = when (val cause = throwable.cause) {
              null, throwable -> null

              else -> from(workerName, cause)
            },
        )
  }
}
