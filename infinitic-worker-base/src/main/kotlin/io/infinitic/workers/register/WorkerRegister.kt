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
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag

interface WorkerRegister {

    val registry: WorkerRegistry

    /**
     * Register task
     */
    fun registerService(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: ServiceFactory,
        tagEngine: TaskTag?
    )

    fun registerService(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: ServiceFactory
    ) = registerService(name, concurrency, retry, factory, null)

    /**
     * Register workflow
     */
    fun registerWorkflow(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: WorkflowFactory,
        engine: WorkflowEngine?,
        tagEngine: WorkflowTag?
    )

    fun registerWorkflow(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: WorkflowFactory
    ) = registerWorkflow(name, concurrency, retry, factory, null, null)

    fun registerWorkflow(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: WorkflowFactory,
        engine: WorkflowEngine
    ) = registerWorkflow(name, concurrency, retry, factory, engine, null)

    fun registerWorkflow(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: WorkflowFactory,
        tagEngine: WorkflowTag
    ) = registerWorkflow(name, concurrency, retry, factory, null, tagEngine)
}
