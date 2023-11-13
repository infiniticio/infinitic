/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.tests

import io.infinitic.clients.InfiniticClient
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.inMemory.InMemoryInfiniticClient
import io.infinitic.inMemory.InMemoryInfiniticWorker
import io.infinitic.pulsar.PulsarInfiniticClient
import io.infinitic.pulsar.PulsarInfiniticService
import io.infinitic.pulsar.config.ClientConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.register.InfiniticRegisterImpl
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

object WorkflowTests {
  private val workerConfig = WorkerConfig.fromResource("/pulsar.yml")
  private val workerRegister = InfiniticRegisterImpl(workerConfig)
  private val clientConfig = ClientConfig.fromResource("/pulsar.yml")

  val worker =
      InfiniticWorker(
          when (workerConfig.transport) {
            Transport.pulsar ->
              PulsarInfiniticService(
                  workerRegister,
                  workerConfig.pulsar!!.client,
                  workerConfig.pulsar!!.admin,
                  workerConfig.pulsar!!,
              )

            Transport.inMemory -> InMemoryInfiniticWorker(workerRegister)
          },
      )

  val client =
      InfiniticClient(
          when (clientConfig.transport) {
            Transport.pulsar -> PulsarInfiniticClient.fromConfig(clientConfig)
            Transport.inMemory -> InMemoryInfiniticClient(workerRegister)
          },
      )

  suspend fun testWorkflowStateEmpty(
    name: String = client.lastDeferred!!.name,
    id: String = client.lastDeferred!!.id
  ) {
    // note: storage is updated after having send messages
    // that's why we are waiting here a bit before checking its value
    delay(100)

    workerRegister.registry.workflowEngines[WorkflowName(name)]!!
        .storage
        .getState(WorkflowId(id)) shouldBe null
  }
}
