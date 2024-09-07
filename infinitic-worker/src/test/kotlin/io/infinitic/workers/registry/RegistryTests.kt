package io.infinitic.workers.registry

import io.cloudevents.CloudEvent
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.EventListenerConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class RegistryTests : StringSpec(
    {
      class TestEventListener : CloudEventListener {
        override fun onEvent(event: CloudEvent) {}
      }

      val transport = Transport.inMemory

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

        worker.getEventListenersConfig() shouldBe eventListener
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
