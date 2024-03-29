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
package io.infinitic.common.tasks.executors.errors

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.cloudEvents.ERROR_CAUSE
import io.infinitic.cloudEvents.ERROR_MESSAGE
import io.infinitic.cloudEvents.ERROR_NAME
import io.infinitic.cloudEvents.ERROR_STACKTRACE
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.utils.toJson
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.exceptions.WorkerException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Data class representing an error */
@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
@AvroName("WorkerError")
data class ExecutionError(
  /** Name of the worker */
  val workerName: WorkerName,

  /** Name of the error */
  val name: String,

  /** Message of the error */
  val message: String?,

  /** String version of the stack trace */
  val stackTraceToString: String,

  /** cause of the error */
  val cause: ExecutionError?
) : JsonAble {
  companion object {
    fun from(exception: WorkerException): ExecutionError =
        ExecutionError(
            workerName = WorkerName(exception.workerName),
            name = exception.name,
            message = exception.message,
            stackTraceToString = exception.stackTraceToString,
            cause = exception.cause?.let { from(it) },
        )

    fun from(workerName: WorkerName, throwable: Throwable): ExecutionError =
        ExecutionError(
            workerName = workerName,
            name = throwable::class.java.name,
            message = throwable.message,
            stackTraceToString = throwable.stackTraceToString(),
            cause = when (val cause = throwable.cause) {
              null, throwable -> null
              else -> from(workerName, cause)
            },
        )
  }

  // we remove end of line for stackTraceToString of the output to preserve logs
  override fun toString(): String = this::class.java.simpleName + "(" +
      listOf(
          "name" to name,
          "message" to message,
          "stackTrace" to stackTraceToString.replace("\n", ""),
          "cause" to cause,
      ).joinToString { "${it.first}=${it.second}" } + ")"

  override fun toJson(): JsonObject = JsonObject(
      mapOf(
          ERROR_NAME to JsonPrimitive(name),
          ERROR_MESSAGE to JsonPrimitive(message),
          ERROR_STACKTRACE to JsonPrimitive(stackTraceToString),
          ERROR_CAUSE to cause.toJson(),
      ),
  )
}
