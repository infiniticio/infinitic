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
import io.infinitic.common.clients.InfiniticClient
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engines.SendToTaskEngine
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import kotlinx.coroutines.CoroutineScope

interface WorkerStarter {
    fun CoroutineScope.startClientResponse(client: InfiniticClient)

    fun CoroutineScope.startWorkflowTag(workflowName: WorkflowName, workflowTagStorage: WorkflowTagStorage, concurrency: Int)

    fun CoroutineScope.startWorkflowEngine(workflowName: WorkflowName, workflowStateStorage: WorkflowStateStorage, concurrency: Int)

    fun CoroutineScope.startWorkflowDelay(workflowName: WorkflowName, concurrency: Int)

    fun CoroutineScope.startTaskTag(taskName: TaskName, taskTagStorage: TaskTagStorage, concurrency: Int)

    fun CoroutineScope.startTaskEngine(taskName: TaskName, taskStateStorage: TaskStateStorage, concurrency: Int)

    fun CoroutineScope.startTaskDelay(taskName: TaskName, concurrency: Int)

    fun CoroutineScope.startTaskExecutor(taskName: TaskName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory)

    fun CoroutineScope.startWorkflowTaskEngine(workflowName: WorkflowName, taskStateStorage: TaskStateStorage, concurrency: Int)

    fun CoroutineScope.startWorkflowTaskDelay(workflowName: WorkflowName, concurrency: Int)

    fun CoroutineScope.startWorkflowTaskExecutor(workflowName: WorkflowName, concurrency: Int, workerRegister: WorkerRegister, clientFactory: ClientFactory)

    val sendToTaskTag: SendToTaskTag

    val sendToTaskEngine: SendToTaskEngine

    val sendToWorkflowTag: SendToWorkflowTag

    val sendToWorkflowEngine: SendToWorkflowEngine
}
