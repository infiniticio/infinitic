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

package io.infinitic.tests.workflows.inMemory

import io.infinitic.client.Client
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.worker.Worker
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.engine.workflowManager.engines.ForWorkflowTaskEngine
import io.infinitic.engine.workflowManager.engines.WorkflowEngine
import io.infinitic.messaging.api.dispatcher.inMemory.InMemoryDispatcher

// class InMemoryDispatcherTest(storage: InMemoryStorageTest) : InMemoryAvroDispatcher() {
class InMemoryDispatcherTest(storage: InMemoryStorageTest) : InMemoryDispatcher() {
    val client = Client(this)
    val worker = Worker(this)
    val taskEngine = ForWorkflowTaskEngine(storage, this)
    val workflowEngine = WorkflowEngine(storage, this)
    val monitoringPerName = MonitoringPerName(storage, this)
    val monitoringGlobal = MonitoringGlobal(storage)

    var taskStatus: TaskStatus? = null
    var workflowOutput: Any? = null

    init {
        workflowEngineHandle = {
            workflowEngine.handle(it)

            when (it) {
                is WorkflowCompleted -> { workflowOutput = it.workflowOutput.data }
            }
        }
        taskEngineHandle = {
            taskEngine.handle(it)
        }
        monitoringPerNameHandle = {
            monitoringPerName.handle(it)
            when (it) {
                is TaskStatusUpdated -> { taskStatus = it.newStatus }
            }
        }
        monitoringGlobalHandle = { monitoringGlobal.handle(it) }
        workerHandle = {
            worker.handle(it)
        }
    }

    fun reset() {
        taskStatus = null
        workflowOutput = null
    }
}
