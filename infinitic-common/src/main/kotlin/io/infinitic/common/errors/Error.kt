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

import io.infinitic.exceptions.workflows.WorkflowRunException
import kotlinx.serialization.Serializable

/**
 * Data class representing an error
 */
@Serializable
data class Error(
    /**
     * Name of the error
     */
    val errorName: String,

    /**
     * Message of the error
     */
    val errorMessage: String?,

    /**
     * Except for CanceledDeferredException, FailedDeferredException, TimedOutDeferredException
     * String version of the stack trace
     */
    val errorStackTraceToString: String?,

    /**
     * for CanceledDeferredException, FailedDeferredException, TimedOutDeferredException
     * id of the failing task or workflow where the error occurred
     */
    val whereId: String? = null,

    /**
     * for CanceledDeferredException, FailedDeferredException, TimedOutDeferredException
     * name of the failing task or child workflow where the error occurred
     */
    val whereName: String? = null,

    /**
     * cause of the error
     */
    val errorCause: Error?
) {
    companion object {
        fun from(throwable: Throwable): Error = Error(
            errorName = throwable::class.java.name,
            errorMessage = throwable.message,
            errorStackTraceToString = when (throwable) {
                is WorkflowRunException -> null
                else -> throwable.stackTraceToString()
            },
            whereId = when (throwable) {
                is WorkflowRunException -> throwable.id
                else -> null
            },
            whereName = when (throwable) {
                is WorkflowRunException -> throwable.name
                else -> null
            },
            errorCause = when (val cause = throwable.cause) {
                throwable, null -> null
                else -> from(cause)
            }
        )
    }

    override fun toString(): String = mapOf(
        "errorName" to errorName,
        "errorMessage" to errorMessage,
        "whereId" to whereId,
        "whereName" to whereName,
        "errorCause" to errorCause.toString()
    ).toString()
}
