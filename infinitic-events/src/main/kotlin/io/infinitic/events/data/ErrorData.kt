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

package io.infinitic.events.data

import io.infinitic.common.tasks.executors.errors.ExecutionError
import kotlinx.serialization.Serializable


/**
 * Data class representing error data, used for avoiding
 * leaking the internal representation of ExecutionError
 *
 * @property name The name of the error.
 * @property message The message of the error.
 * @property stackTrace The string representation of the stack trace.
 * @property cause The cause of the error, if any.
 */
@Serializable
data class ErrorData(
  val name: String,
  val message: String,
  val stackTrace: String,
  val cause: ErrorData?
)

fun ExecutionError.toErrorData(): ErrorData = ErrorData(
    name = name,
    message = message ?: "",
    stackTrace = stackTraceToString,
    cause = cause?.toErrorData(),
)
