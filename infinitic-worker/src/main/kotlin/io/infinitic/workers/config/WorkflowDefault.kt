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

package io.infinitic.workers.config

import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.workers.register.WorkerRegister
import io.infinitic.workflows.WorkflowCheckMode

data class WorkflowDefault(
    val concurrency: Int = WorkerRegister.DEFAULT_CONCURRENCY,
    val timeoutInSeconds: Double? = null,
    val retry: RetryPolicy? = null,
    val checkMode: WorkflowCheckMode? = null
) {
    init {
        require(concurrency >= 0) { "default workflow concurrency must be positive" }

        if (timeoutInSeconds != null) {
            require(timeoutInSeconds > 0) { "default workflow timeoutSeconds must be positive" }
        }
    }
}
