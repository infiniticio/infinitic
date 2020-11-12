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

package io.infinitic.engines.pulsar.main

import io.infinitic.engines.pulsar.extensions.newMonitoringGlobalConsumer
import io.infinitic.engines.pulsar.extensions.newMonitoringPerNameConsumer
import io.infinitic.engines.pulsar.extensions.newTaskEngineConsumer
import io.infinitic.engines.pulsar.extensions.newWorkflowEngineConsumer
import io.infinitic.engines.monitoringGlobal.engine.MonitoringGlobalEngine
import io.infinitic.engines.monitoringGlobal.storage.MonitoringGlobalStateStorage
import io.infinitic.engines.monitoringPerName.engine.MonitoringPerNameEngine
import io.infinitic.common.SendToMonitoringGlobal
import io.infinitic.engines.monitoringPerName.storage.MonitoringPerNameStateStorage
import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.engines.tasks.storage.TaskStateStorage
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.engines.workflows.engine.WorkflowEngine
import io.infinitic.engines.workflows.engine.taskEngineInWorkflowEngine
import io.infinitic.engines.workflows.storage.WorkflowStateStorage
import io.infinitic.messaging.pulsar.extensions.startConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class Application(
    private val pulsarClient: PulsarClient,
    private val workflowStateStorage: WorkflowStateStorage,
    private val taskStateStorage: TaskStateStorage,
    private val monitoringPerNameStateStorage: MonitoringPerNameStateStorage,
    private val monitoringGlobalStateStorage: MonitoringGlobalStateStorage,
    private val sendToWorkflowEngine: SendToWorkflowEngine,
    private val sendToTaskEngine: SendToTaskEngine,
    private val sendToMonitoringPerName: SendToMonitoringPerName,
    private val sendToMonitoringGlobal: SendToMonitoringGlobal,
    private val sendToWorkers: SendToWorkers
    ) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                stop()
            }
        )
    }

    fun run() {
        val workflowEngine = WorkflowEngine(
            workflowStateStorage,
            sendToWorkflowEngine,
            sendToTaskEngine
        )
        val taskEngine = taskEngineInWorkflowEngine(
            taskStateStorage,
            sendToWorkflowEngine,
            sendToTaskEngine,
            sendToMonitoringPerName,
            sendToWorkers
        )
        val monitoringPerName = MonitoringPerNameEngine(
            monitoringPerNameStateStorage,
            sendToMonitoringGlobal
        )
        val monitoringGlobal = MonitoringGlobalEngine(
            monitoringGlobalStateStorage
        )

        try {
            val taskEngineConsumer = pulsarClient.newTaskEngineConsumer()
            val monitoringPerNameConsumer = pulsarClient.newMonitoringPerNameConsumer()
            val monitoringGlobalConsumer = pulsarClient.newMonitoringGlobalConsumer()
            val workflowEngineConsumer = pulsarClient.newWorkflowEngineConsumer()

            startConsumer(workflowEngineConsumer) {
                workflowEngine.handle(it.value.message())
            }

            startConsumer(taskEngineConsumer) {
                taskEngine.handle(it.value.message())
            }

            startConsumer(monitoringPerNameConsumer) {
                monitoringPerName.handle(it.value.message())
            }

            startConsumer(monitoringGlobalConsumer) {
                monitoringGlobal.handle(it.value.message())
            }
        } catch (e: PulsarClientException) {
            println(e) // FIXME: Remove and replace by a logger
            pulsarClient.close()
        }
    }

    fun stop() {
        cancel()
        pulsarClient.close()
    }
}
