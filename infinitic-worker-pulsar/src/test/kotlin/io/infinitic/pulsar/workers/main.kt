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

import io.infinitic.client.Client
import io.infinitic.pulsar.transport.PulsarClientOutput
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.tests.samples.TaskA
import io.infinitic.workflows.tests.samples.TaskAImpl
import io.infinitic.workflows.tests.samples.WorkflowA
import io.infinitic.workflows.tests.samples.WorkflowAImpl
import io.infinitic.workflows.tests.samples.WorkflowB
import io.infinitic.workflows.tests.samples.WorkflowBImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClient

fun main() {
    val pulsarClient = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()

    runBlocking {
        val client = Client(PulsarClientOutput.from(pulsarClient))

        val taskExecutorRegister = TaskExecutorRegisterImpl().apply {
            register(TaskA::class.java.name) { TaskAImpl() }
            register(WorkflowA::class.java.name) { WorkflowAImpl() }
            register(WorkflowB::class.java.name) { WorkflowBImpl() }
        }

        startPulsar(taskExecutorRegister, pulsarClient, InMemoryStorage())

        repeat(1000) {
//            client.dispatch<TaskA> { reverse("abc") }
            client.dispatch(WorkflowA::class.java) { seq1() }
            delay(100)
        }
    }
}
