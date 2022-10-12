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

package io.infinitic.workers.register

import io.infinitic.common.workers.ServiceFactory
import io.infinitic.common.workers.WorkflowFactory
import io.infinitic.common.workers.config.RetryExponentialBackoff
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag

interface WorkerRegister {

    val registry: WorkerRegistry

    companion object {
        const val DEFAULT_CONCURRENCY = 1
        val DEFAULT_WORKFLOW_TIMEOUT = WithTimeout { 30.0 }
        val DEFAULT_TASK_TIMEOUT = null
        val DEFAULT_TASK_RETRY_POLICY = RetryExponentialBackoff().apply { isDefault = true }
        val DEFAULT_WORKFLOW_TASK_RETRY_POLICY = null
        val DEFAULT_TASK_TAG = TaskTag().apply { isDefault = true }
        val DEFAULT_WORKFLOW_ENGINE = WorkflowEngine().apply { isDefault = true }
        val DEFAULT_WORKFLOW_TAG = WorkflowTag().apply { isDefault = true }
    }

    /**
     * Register task
     */
    fun registerService(
        name: String,
        factory: ServiceFactory,
        concurrency: Int = DEFAULT_CONCURRENCY,
        timeout: WithTimeout? = DEFAULT_TASK_TIMEOUT,
        retry: WithRetry? = DEFAULT_TASK_RETRY_POLICY,
        tagEngine: TaskTag? = DEFAULT_TASK_TAG
    )

    /**
     * Register workflow
     */
    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int = DEFAULT_CONCURRENCY,
        timeout: WithTimeout? = DEFAULT_WORKFLOW_TIMEOUT,
        retry: WithRetry? = DEFAULT_WORKFLOW_TASK_RETRY_POLICY,
        engine: WorkflowEngine? = DEFAULT_WORKFLOW_ENGINE,
        tagEngine: WorkflowTag? = DEFAULT_WORKFLOW_TAG
    )
}
