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
package io.infinitic.workers.config

import io.cloudevents.CloudEvent
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class ConfigGetterInterfaceTests : StringSpec(
    {
      class TestEventListener : CloudEventListener {
        override fun onEvent(event: CloudEvent) {}
      }

      val transport = InMemoryTransportConfig()

      val eventListener = EventListenerConfig.builder()
          .setListener(TestEventListener())
          .build()

      val storage = InMemoryStorageConfig(InMemoryConfig())

      val serviceName = ServiceA::class.java.name

      val serviceTagEngine = ServiceTagEngineConfig.builder()
          .setServiceName(serviceName)
          .setStorage(storage)
          .build()

      val serviceExecutor = ServiceExecutorConfig.builder()
          .setServiceName(serviceName)
          .setFactory { ServiceAImpl() }
          .build()

      val workflowName = WorkflowA::class.java.name

      val workflowTagEngine = WorkflowTagEngineConfig.builder()
          .setWorkflowName(workflowName)
          .setStorage(storage)
          .build()

      val workflowExecutor = WorkflowExecutorConfig.builder()
          .setWorkflowName(workflowName)
          .addFactory { WorkflowAImpl() }
          .build()

      val workflowStateEngine = WorkflowStateEngineConfig.builder()
          .setWorkflowName(workflowName)
          .setStorage(storage)
          .build()

      "Can access EventListenerConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .setEventListener(eventListener)
            .build()

        worker.getEventListenerConfig() shouldBe eventListener
      }

      "Can access ServiceExecutorConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceExecutor(serviceExecutor)
            .build()

        worker.getServiceExecutorConfig(serviceName) shouldBe serviceExecutor
      }

      "Can access ServiceTagEngineConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceTagEngine(serviceTagEngine)
            .build()

        worker.getServiceTagEngineConfig(serviceName) shouldBe serviceTagEngine
      }

      "Can access WorkflowExecutorConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowExecutor(workflowExecutor)
            .build()

        worker.getWorkflowExecutorConfig(workflowName) shouldBe workflowExecutor
      }

      "Can access WorkflowStateEngineConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowStateEngine(workflowStateEngine)
            .build()

        worker.getWorkflowStateEngineConfig(workflowName) shouldBe workflowStateEngine
      }

      "Can access WorkflowTagEngineConfig" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowTagEngine(workflowTagEngine)
            .build()

        worker.getWorkflowTagEngineConfig(workflowName) shouldBe workflowTagEngine
      }
    },
)
