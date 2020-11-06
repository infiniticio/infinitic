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

package io.infinitic.engine.pulsar.main

import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.engine.pulsar.extensions.newMonitoringGlobalConsumer
import io.infinitic.engine.pulsar.extensions.newMonitoringPerNameConsumer
import io.infinitic.engine.pulsar.extensions.newTaskEngineConsumer
import io.infinitic.engine.pulsar.extensions.newWorkflowEngineConsumer
import io.infinitic.engine.pulsar.extensions.startConsumer
import io.infinitic.engine.monitoringGlobal.engine.MonitoringGlobal
import io.infinitic.engine.monitoringPerName.engine.MonitoringPerName
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.engine.workflowManager.engines.ForWorkflowTaskEngine
import io.infinitic.engine.workflowManager.engines.WorkflowEngine
import io.infinitic.engine.workflowManager.storages.WorkflowStateStorage
import io.infinitic.messaging.api.dispatcher.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class Application private constructor(
    private val pulsarClient: PulsarClient,
    private val taskStateStorage: TaskStateStorage,
    private val workflowStateStorage: WorkflowStateStorage,
    private val dispatcher: Dispatcher
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
        val taskEngine = ForWorkflowTaskEngine(taskStateStorage, dispatcher)
        val monitoringPerName = MonitoringPerName(taskStateStorage, dispatcher)
        val monitoringGlobal = MonitoringGlobal(taskStateStorage)
        val workflowEngine = WorkflowEngine(workflowStateStorage, dispatcher)

        try {
            val taskEngineConsumer = pulsarClient.newTaskEngineConsumer()
            val monitoringPerNameConsumer = pulsarClient.newMonitoringPerNameConsumer()
            val monitoringGlobalConsumer = pulsarClient.newMonitoringGlobalConsumer()
            val workflowEngineConsumer = pulsarClient.newWorkflowEngineConsumer()

            startConsumer(workflowEngineConsumer) { message ->
                message.value
                    .let { io.infinitic.common.workflows.avro.AvroConverter.fromWorkflowEngine(it) }
                    .let { workflowEngine.handle(it) }
            }
            startConsumer(taskEngineConsumer) { message ->
                message.value
                    .let { AvroConverter.fromTaskEngine(it) }
                    .let { taskEngine.handle(it) }
            }
            startConsumer(monitoringPerNameConsumer) { message ->
                message.value
                    .let { AvroConverter.fromMonitoringPerName(it) }
                    .let { monitoringPerName.handle(it) }
            }
            startConsumer(monitoringGlobalConsumer) { message ->
                message.value
                    .let { AvroConverter.fromMonitoringGlobal(it) }
                    .let { monitoringGlobal.handle(it) }
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

    companion object {
        fun create(pulsarClient: PulsarClient, taskStateStorage: TaskStateStorage, workflowStateStorage: WorkflowStateStorage, dispatcher: Dispatcher) = Application(pulsarClient, taskStateStorage, workflowStateStorage, dispatcher)
    }
}
