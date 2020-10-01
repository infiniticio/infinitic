// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.worker.task

import io.infinitic.common.tasks.data.TaskOptions
import java.lang.reflect.Method

internal data class TaskCommand(
    val job: Any,
    val method: Method,
    val input: Array<out Any?>,
    val options: TaskOptions
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskCommand

        if (job != other.job) return false
        if (method != other.method) return false
        if (!input.contentEquals(other.input)) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = job.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + input.contentHashCode()
        result = 31 * result + options.hashCode()
        return result
    }
}
