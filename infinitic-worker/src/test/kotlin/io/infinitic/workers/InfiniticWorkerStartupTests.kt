/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
package io.infinitic.workers

import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.config.ServiceConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.infinitic.workflows.WorkflowCheckMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay

class InfiniticWorkerStartupTests : StringSpec(
    {
      // Test data - using fully qualified interface names to match the actual implementations
      val serviceName = ServiceA::class.qualifiedName!!
      val workflowName = WorkflowA::class.qualifiedName!!

      // Common configurations
      val transportConfig = InMemoryTransportConfig()
      val storageConfig = InMemoryStorageConfig(InMemoryConfig())

      val serviceTagEngineConfig = ServiceTagEngineConfig.builder()
          .setServiceName(serviceName)
          .setStorage(storageConfig)
          .setConcurrency(5)
          .build()

      val serviceExecutorConfig = ServiceExecutorConfig.builder()
          .setServiceName(serviceName)
          .setFactory { ServiceAImpl() }
          .setConcurrency(3)
          .setEventHandlerConcurrency(2)
          .setRetryHandlerConcurrency(1)
          .setBatch(maxMessages = 10, maxSeconds = 1000.0)
          .build()

      val workflowTagEngineConfig = WorkflowTagEngineConfig(
          workflowName = workflowName,
          storage = storageConfig,
          concurrency = 4,
          batch = BatchConfig(maxMessages = 5, maxSeconds = 500.0),
      )

      val workflowStateEngineConfig = WorkflowStateEngineConfig(
          workflowName = workflowName,
          storage = storageConfig,
          concurrency = 3,
          commandHandlerConcurrency = 3,
          eventHandlerConcurrency = 2,
          timerHandlerConcurrency = 1,
          batch = BatchConfig(maxMessages = 10, maxSeconds = 1000.0),
      )

      val workflowExecutorConfig = WorkflowExecutorConfig.builder()
          .setWorkflowName(workflowName)
          .addFactory { WorkflowAImpl() }
          .setConcurrency(3)
          .setEventHandlerConcurrency(2)
          .setRetryHandlerConcurrency(1)
          .setBatch(maxMessages = 10, maxSeconds = 1000.0)
          .setCheckMode(WorkflowCheckMode.strict)
          .build()

      val eventListenerConfig = EventListenerConfig.builder()
          .setListener({ _ -> })
          .setConcurrency(2)
          .build()

      "startEventListener should be called with correct parameters" {
        // Given
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            eventListener = eventListenerConfig,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<EventListenerConfig>()
        verify(exactly = 1) {
          worker["startEventListener"](capture(capturedConfig))
        }

        capturedConfig shouldBe mutableListOf(eventListenerConfig)
      }

      "startServiceTagEngine should be called with correct parameters" {
        // Given
        val services = listOf(
            ServiceConfig(
                name = serviceName,
                tagEngine = serviceTagEngineConfig,
            ),
        )
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            services = services,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<ServiceTagEngineConfig>()
        verify(exactly = services.size) {
          worker["startServiceTagEngine"](capture(capturedConfig))
        }

        capturedConfig shouldBe services.map { it.tagEngine }
      }

      "startServiceExecutor should be called with correct parameters" {
        // Given
        val services = listOf(
            ServiceConfig(
                name = serviceName,
                executor = serviceExecutorConfig,
            ),
        )
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            services = services,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<ServiceExecutorConfig>()
        verify(exactly = services.size) {
          worker["startServiceExecutor"](capture(capturedConfig))
        }

        capturedConfig shouldBe services.map { it.executor }
      }

      "startWorkflowTagEngine should be called with correct parameters" {
        val workflows = listOf(
            WorkflowConfig(
                name = workflowName,
                tagEngine = workflowTagEngineConfig,
            ),
        )
        // Given
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            workflows = workflows,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<WorkflowTagEngineConfig>()
        verify(exactly = workflows.size) {
          worker["startWorkflowTagEngine"](capture(capturedConfig))
        }

        capturedConfig shouldBe workflows.map { it.tagEngine }
      }

      "startWorkflowStateEngine should be called with correct parameters" {
        // Given
        val workflows = listOf(
            WorkflowConfig(
                name = workflowName,
                stateEngine = workflowStateEngineConfig,
            ),
        )
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            workflows = workflows,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<WorkflowStateEngineConfig>()
        verify(exactly = workflows.size) {
          worker["startWorkflowStateEngine"](capture(capturedConfig))
        }

        capturedConfig shouldBe workflows.map { it.stateEngine }
      }

      "startWorkflowExecutor should be called with correct parameters" {
        // Given
        val workflows = listOf(
            WorkflowConfig(
                name = workflowName,
                executor = workflowExecutorConfig,
            ),
        )
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            workflows = workflows,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then
        val capturedConfig = mutableListOf<WorkflowExecutorConfig>()
        verify(exactly = workflows.size) {
          worker["startWorkflowExecutor"](capture(capturedConfig))
        }

        capturedConfig shouldBe workflows.map { it.executor }
      }

      "startAsync should start all configured components" {
        val services = listOf(
            ServiceConfig(
                name = serviceName,
                tagEngine = serviceTagEngineConfig,
            ),
            ServiceConfig(
                name = serviceName,
                executor = serviceExecutorConfig,
            ),
        )
        val workflows = listOf(
            WorkflowConfig(
                name = workflowName,
                tagEngine = workflowTagEngineConfig,
            ),
            WorkflowConfig(
                name = workflowName,
                stateEngine = workflowStateEngineConfig,
            ),
            WorkflowConfig(
                name = workflowName,
                executor = workflowExecutorConfig,
            ),
        )
        // Given
        val config = InfiniticWorkerConfig(
            transport = transportConfig,
            eventListener = eventListenerConfig,
            services = services,
            workflows = workflows,
        )

        // Create a spy of the worker to verify method calls
        val worker = spyk(InfiniticWorker(config), recordPrivateCalls = true)

        // When
        worker.startAsync()
        delay(100)

        // Then verify all expected methods were called with the correct configurations
        verify(exactly = 1) { worker["startEventListener"](any<EventListenerConfig>()) }
        verify(exactly = 1) { worker["startServiceExecutor"](any<ServiceExecutorConfig>()) }
        verify(exactly = 1) { worker["startServiceTagEngine"](any<ServiceTagEngineConfig>()) }
        verify(exactly = 1) { worker["startWorkflowTagEngine"](any<WorkflowTagEngineConfig>()) }
        verify(exactly = 1) { worker["startWorkflowStateEngine"](any<WorkflowStateEngineConfig>()) }
        verify(exactly = 1) { worker["startWorkflowExecutor"](any<WorkflowExecutorConfig>()) }
      }
    },
)
