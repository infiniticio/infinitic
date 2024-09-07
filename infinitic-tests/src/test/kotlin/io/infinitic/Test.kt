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
package io.infinitic

import io.infinitic.common.fixtures.DockerOnly
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.transport.config.Transport
import io.infinitic.utils.Listener
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import io.kotest.core.spec.AutoScan
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


/**
 * This singleton provides the client and the worker used in tests
 * If Docker is available, the tests are done on Pulsar, if not they are done in memory
 * Docker is available on GitHub.
 */
internal object Test {
  private val pulsarServer = DockerOnly().pulsarServer

  private val workerConfig =
      InfiniticWorkerConfig.fromYamlResource("/pulsar.yml", "/register.yml").let {
        when (pulsarServer) {
          null -> it.copy(transport = Transport.inMemory)
          else -> it.copy(
              transport = Transport.pulsar,
              pulsar = it.pulsar!!.copy(
                  brokerServiceUrl = pulsarServer.pulsarBrokerUrl,
                  webServiceUrl = pulsarServer.httpServiceUrl,
                  policies = it.pulsar!!.policies.copy(delayedDeliveryTickTimeMillis = 1), // useful for tests
              ),
          )
        }
      }

  val worker = InfiniticWorker.fromConfig(workerConfig)
  val client = worker.client

  fun start() {
    worker.startAsync()
  }

  fun stop() {
    worker.close()
    client.close()
    pulsarServer?.stop()
  }
}


/**
 * This listener is used to close resources after all tests
 */
@AutoScan
internal class BeforeListener : BeforeProjectListener {
  override suspend fun beforeProject() {
    Test.start()
  }
}

/**
 * This listener is used to close resources after all tests
 */
@AutoScan
internal class AfterListener : AfterProjectListener {
  override suspend fun afterProject() {
    Test.stop()
  }
}

/**
 * Utility to retrieve a workflow state
 * If no parameter is provided, it returns the state of the last workflow used
 */
internal suspend fun InfiniticWorker.getWorkflowState(
  name: String = client.lastDeferred!!.name,
  id: String = client.lastDeferred!!.id
): WorkflowState? {
  // note: storage is updated after sending messages
  // that's why we are waiting here a bit before getting the state
  delay(200)

  return getWorkflowStateEngineConfig(name)?.workflowStateStorage?.getState(WorkflowId(id))
}

object ProjectConfig : AbstractProjectConfig() {
  // each test should not be longer than 5s
  override val timeout = 5000.milliseconds

  override suspend fun beforeProject() {
    Listener.clear()
  }

  override suspend fun afterProject() {
    Listener.print()
  }
}
