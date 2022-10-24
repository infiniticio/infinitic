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

package io.infinitic.common.workers

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import kotlinx.coroutines.CoroutineScope

interface WorkerStarter {
    fun CoroutineScope.startWorkflowTag(
        workflowName: WorkflowName,
        workflowTagStorage: WorkflowTagStorage,
        concurrency: Int
    )

    fun CoroutineScope.startWorkflowEngine(
        workflowName: WorkflowName,
        workflowStateStorage: WorkflowStateStorage,
        concurrency: Int
    )

    fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int)

    fun CoroutineScope.startTaskTag(serviceName: ServiceName, taskTagStorage: TaskTagStorage, concurrency: Int)

    fun CoroutineScope.startTaskExecutor(
        serviceName: ServiceName,
        concurrency: Int,
        workerRegistry: WorkerRegistry,
        clientFactory: ClientFactory
    )

    fun CoroutineScope.startWorkflowTaskExecutor(
        workflowName: WorkflowName,
        concurrency: Int,
        workerRegistry: WorkerRegistry,
        clientFactory: ClientFactory
    )
}
