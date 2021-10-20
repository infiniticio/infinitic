/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.errors

import kotlinx.serialization.Serializable

/**
 * Data class representing an error
 */
@Serializable
data class RuntimeError(
    /**
     * Name of the error
     */
    val name: String,

    /**
     * Message of the error
     */
    val message: String?,

    /**
     * String version of the stack trace
     */
    val stackTraceToString: String,

    /**
     * cause of the error
     */
    val cause: RuntimeError?
) {
    companion object {
        fun from(throwable: Throwable): RuntimeError = RuntimeError(
            name = throwable::class.java.name,
            message = throwable.message,
            stackTraceToString = throwable.stackTraceToString(),
            cause = when (val cause = throwable.cause) {
                null, throwable -> null
                else -> from(cause)
            }
        )
    }

    // removing stackTraceToString of the output to preserve logs
    override fun toString(): String = this::class.java.simpleName + "(" + listOf(
        "name" to name,
        "message" to message,
        "cause" to cause
    ).joinToString() { "${it.first}=${it.second}" } + ")"
}
