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
package io.infinitic.tasks.executor.task

import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout

data class TaskContextImpl(
    override val workerName: String,
    override val workerRegistry: WorkerRegistry,
    override val serviceName: ServiceName,
    override val taskId: TaskId,
    override val taskName: MethodName,
    override val workflowId: WorkflowId?,
    override val workflowName: WorkflowName?,
    override val workflowVersion: WorkflowVersion?,
    override val retrySequence: TaskRetrySequence,
    override val retryIndex: TaskRetryIndex,
    override val lastError: ExecutionError?,
    override val tags: Set<String>,
    override val meta: MutableMap<String, ByteArray>,
    override val withTimeout: WithTimeout?,
    override val withRetry: WithRetry?,
    private val clientFactory: ClientFactory
) : TaskContext {
  override val client: InfiniticClientInterface by lazy { clientFactory() }
}
