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

package io.infinitic.pulsar.workers

import io.infinitic.client.InfiniticClient
import io.infinitic.pulsar.samples.TaskA
import io.infinitic.pulsar.samples.TaskAImpl
import io.infinitic.pulsar.samples.WorkflowA
import io.infinitic.pulsar.samples.WorkflowAImpl
import io.infinitic.pulsar.samples.WorkflowB
import io.infinitic.pulsar.samples.WorkflowBImpl
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutputs
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClient

fun main() {
    val pulsarClient = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val tenant = "infinitic"
    val namespace = "dev"

    val consumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
    val pulsarOutputFactory = PulsarOutputs.from(pulsarClient, tenant, namespace)

    runBlocking {
        val client = InfiniticClient(PulsarOutputs.from(pulsarClient, tenant, namespace).clientOutput)

        val taskExecutorRegister = TaskExecutorRegisterImpl().apply {
            register(TaskA::class.java.name) { TaskAImpl() }
            register(WorkflowA::class.java.name) { WorkflowAImpl() }
            register(WorkflowB::class.java.name) { WorkflowBImpl() }
        }

        startPulsarWorkers(consumerFactory, pulsarOutputFactory, taskExecutorRegister, InMemoryStorage())

        repeat(1000) {
            client.startTask<TaskA> { reverse("abc") }
            client.startWorkflow<WorkflowA> { seq1() }
            delay(100)
        }
    }
}
