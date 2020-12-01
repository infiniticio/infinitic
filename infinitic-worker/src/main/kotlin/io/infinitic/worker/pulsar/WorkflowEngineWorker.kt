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

package io.infinitic.worker.pulsar

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.pulsar.extensions.newWorkflowEngineConsumer
import io.infinitic.pulsar.extensions.startConsumer
import io.infinitic.pulsar.transport.PulsarTransport
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.WorkflowStateKeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class WorkflowEngineWorker(storage: KeyValueStorage) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    private val workflowStateStorage = WorkflowStateKeyValueStorage(storage)
    private var pulsarClient: PulsarClient? = null

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                stop()
            }
        )
    }

    fun stop() {
        cancel()
        pulsarClient?.close()
    }

    fun start(pulsarClient: PulsarClient) {
        this.pulsarClient = pulsarClient

        val transport = PulsarTransport.from(pulsarClient)

        val workflowEngine = WorkflowEngine(
            workflowStateStorage,
            transport.sendToWorkflowEngine,
            transport.sendToTaskEngine
        )

        try {
            val workflowEngineConsumer = pulsarClient.newWorkflowEngineConsumer()

            startConsumer(workflowEngineConsumer) {
                workflowEngine.handle(it.value.message())
            }
        } catch (e: PulsarClientException) {
            println(e) // FIXME: Remove and replace by a logger
            pulsarClient.close()
        }
    }
}