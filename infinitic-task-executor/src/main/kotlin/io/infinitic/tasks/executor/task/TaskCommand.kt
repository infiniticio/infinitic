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

package io.infinitic.tasks.executor.task

import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import java.lang.reflect.Method

internal data class TaskCommand(
    val service: Any,
    val method: Method,
    val parameters: Array<Any?>,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskCommand

        if (service != other.service) return false
        if (method != other.method) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (withTimeout != other.withTimeout) return false
        if (withRetry != other.withRetry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = service.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + (withTimeout?.hashCode() ?: 0)
        result = 31 * result + (withRetry?.hashCode() ?: 0)
        return result
    }
}
