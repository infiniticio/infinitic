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
import io.infinitic.common.workers.config.WorkflowCheckMode
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
        val DEFAULT_TASK_TAG = TaskTag().apply { isDefault = true }
        val DEFAULT_WORKFLOW_ENGINE = WorkflowEngine().apply { isDefault = true }
        val DEFAULT_WORKFLOW_TAG = WorkflowTag().apply { isDefault = true }
    }

    /**
     * Register service
     */
    fun registerService(
        name: String,
        factory: ServiceFactory,
        concurrency: Int = DEFAULT_CONCURRENCY,
        timeout: WithTimeout? = null,
        retry: WithRetry? = null,
        tagEngine: TaskTag? = DEFAULT_TASK_TAG
    )

    fun registerService(
        name: String,
        factory: ServiceFactory,
        concurrency: Int,
        timeout: WithTimeout?,
        retry: WithRetry?
    ) = registerService(name, factory, concurrency, timeout, retry, DEFAULT_TASK_TAG)

    fun registerService(
        name: String,
        factory: ServiceFactory,
        concurrency: Int,
        timeout: WithTimeout?
    ) = registerService(name, factory, concurrency, timeout, null, DEFAULT_TASK_TAG)

    fun registerService(
        name: String,
        factory: ServiceFactory,
        concurrency: Int
    ) = registerService(name, factory, concurrency, null, null, DEFAULT_TASK_TAG)

    fun registerService(
        name: String,
        factory: ServiceFactory
    ) = registerService(name, factory, DEFAULT_CONCURRENCY, null, null, DEFAULT_TASK_TAG)

    /**
     * Register workflow
     */
    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int = DEFAULT_CONCURRENCY,
        timeout: WithTimeout? = null,
        retry: WithRetry? = null,
        checkMode: WorkflowCheckMode? = null,
        engine: WorkflowEngine? = DEFAULT_WORKFLOW_ENGINE,
        tagEngine: WorkflowTag? = DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int,
        timeout: WithTimeout?,
        retry: WithRetry?,
        checkMode: WorkflowCheckMode?,
        engine: WorkflowEngine?
    ) = registerWorkflow(
        name,
        factory,
        concurrency,
        timeout,
        retry,
        checkMode,
        engine,
        DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int,
        timeout: WithTimeout?,
        retry: WithRetry?,
        checkMode: WorkflowCheckMode?
    ) = registerWorkflow(
        name,
        factory,
        concurrency,
        timeout,
        retry,
        checkMode,
        DEFAULT_WORKFLOW_ENGINE,
        DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int,
        timeout: WithTimeout?,
        retry: WithRetry?
    ) = registerWorkflow(
        name,
        factory,
        concurrency,
        timeout,
        retry,
        null,
        DEFAULT_WORKFLOW_ENGINE,
        DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int,
        timeout: WithTimeout?
    ) = registerWorkflow(
        name,
        factory,
        concurrency,
        timeout,
        null,
        null,
        DEFAULT_WORKFLOW_ENGINE,
        DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory,
        concurrency: Int
    ) = registerWorkflow(
        name,
        factory,
        concurrency,
        null,
        null,
        null,
        DEFAULT_WORKFLOW_ENGINE,
        DEFAULT_WORKFLOW_TAG
    )

    fun registerWorkflow(
        name: String,
        factory: WorkflowFactory
    ) = registerWorkflow(
        name,
        factory,
        DEFAULT_CONCURRENCY,
        null,
        null,
        null,
        DEFAULT_WORKFLOW_ENGINE,
        DEFAULT_WORKFLOW_TAG
    )
}
