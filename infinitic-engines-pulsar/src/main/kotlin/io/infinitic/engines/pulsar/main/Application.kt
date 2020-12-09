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

package io.infinitic.engines.pulsar.main

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.pulsar.consumers.newMonitoringGlobalEngineConsumer
import io.infinitic.pulsar.consumers.newMonitoringPerNameEngineConsumer
import io.infinitic.pulsar.consumers.newTaskEngineConsumer
import io.infinitic.pulsar.consumers.newWorkflowEngineConsumer
import io.infinitic.pulsar.consumers.startConsumer
import io.infinitic.pulsar.transport.PulsarMonitoringPerNameOutput
import io.infinitic.pulsar.transport.PulsarTaskEngineOutput
import io.infinitic.pulsar.transport.PulsarWorkflowEngineOutput
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.events.NoWorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateKeyValueStorage
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
    private val keyValueStorage: KeyValueStorage

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
        val workflowStateStorage = WorkflowStateKeyValueStorage(keyValueStorage)
        val taskStateStorage = TaskStateKeyValueStorage(keyValueStorage)
        val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(keyValueStorage)
        val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(keyValueStorage)

        val workflowEngine = WorkflowEngine(
            workflowStateStorage,
            NoWorkflowEventStorage(),
            PulsarWorkflowEngineOutput.from(pulsarClient)
        )
        val taskEngine = TaskEngine(
            taskStateStorage,
            NoTaskEventStorage(),
            PulsarTaskEngineOutput.from(pulsarClient)
        )
        val monitoringPerName = MonitoringPerNameEngine(
            monitoringPerNameStateStorage,
            PulsarMonitoringPerNameOutput.from(pulsarClient)
        )
        val monitoringGlobal = MonitoringGlobalEngine(
            monitoringGlobalStateStorage
        )

        try {
            val taskEngineConsumer = pulsarClient.newTaskEngineConsumer()
            val monitoringPerNameConsumer = pulsarClient.newMonitoringPerNameEngineConsumer()
            val monitoringGlobalConsumer = pulsarClient.newMonitoringGlobalEngineConsumer()
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
