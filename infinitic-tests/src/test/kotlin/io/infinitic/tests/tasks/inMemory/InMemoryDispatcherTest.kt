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

package io.infinitic.tests.tasks.inMemory

import io.infinitic.messaging.api.dispatcher.inMemory.InMemoryDispatcher
import io.infinitic.client.Client
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.worker.Worker

class InMemoryDispatcherTest(storage: TaskStateStorage) : InMemoryDispatcher() {
    val client = Client(this)
    val worker = Worker(this)
    val taskEngine = TaskEngine(storage, this)
    val monitoringPerName = MonitoringPerName(storage, this)
    val monitoringGlobal = MonitoringGlobal(storage)

    lateinit var taskStatus: TaskStatus

    init {
        taskEngineHandle = { taskEngine.handle(it) }
        monitoringPerNameHandle = {
            monitoringPerName.handle(it)
            when (it) {
                is TaskStatusUpdated -> { taskStatus = it.newStatus }
            }
        }
        monitoringGlobalHandle = { monitoringGlobal.handle(it) }
        workerHandle = { worker.handle(it) }
    }
}
