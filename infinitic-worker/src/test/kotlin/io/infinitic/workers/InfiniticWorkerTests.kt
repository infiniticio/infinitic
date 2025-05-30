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
package io.infinitic.workers

import io.cloudevents.CloudEvent
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.fixtures.later
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.storage.config.MySQLConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

internal class InfiniticWorkerTests : StringSpec(
    {
      val transport = InMemoryTransportConfig()

      class TestEventListener : CloudEventListener {
        override fun onEvents(cloudEvents: List<CloudEvent>) {}
      }

      val eventListener = EventListenerConfig.builder()
          .setListener(TestEventListener())
          .build()

      val storage = InMemoryStorageConfig(InMemoryConfig())

      val serviceName = ServiceA::class.java.name
      val serviceClass = ServiceAImpl::class.java.name

      val serviceTagEngine = ServiceTagEngineConfig.builder()
          .setServiceName(serviceName)
          .setStorage(storage)
          .build()

      val serviceExecutor = ServiceExecutorConfig.builder()
          .setServiceName(serviceName)
          .setFactory { ServiceAImpl() }
          .build()

      val workflowName = WorkflowA::class.java.name
      val workflowClass = WorkflowAImpl::class.java.name

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

      "Can create Infinitic Worker as Event Listener through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .setEventListener(eventListener)
              .build()

        }
        worker.getEventListenerConfig() shouldBe eventListener
      }

      "Can create Infinitic Worker as Event Listener through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
eventListener:
  class: ${TestEventListener::class.java.name}
          """,
          )
        }
        with(worker.getEventListenerConfig()) {
          this?.listener?.shouldBeInstanceOf<TestEventListener>()
        }
      }

      "Can create Infinitic Worker as Service Executor through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .addServiceExecutor(serviceExecutor)
              .build()
        }
        worker.getServiceExecutorConfig(serviceName) shouldBe serviceExecutor
      }

      "Can create Infinitic Worker as Service Executor through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
services:
- name: $serviceName
  executor:
    class: $serviceClass
          """,
          )
        }
        with(worker.getServiceExecutorConfig(serviceName)) {
          this?.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
          this?.serviceName shouldBe serviceName
        }
      }

      "Can create Infinitic Worker as Service Tag Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .addServiceTagEngine(serviceTagEngine)
              .build()
        }
        worker.getServiceTagEngineConfig(serviceName) shouldBe serviceTagEngine
        worker.close()
      }

      "Can create Infinitic Worker as Service Tag Engine through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
services:
- name: $serviceName
  tagEngine:
    storage:
      inMemory:
          """,
          )
        }
        with(worker.getServiceTagEngineConfig(serviceName)) {
          this?.serviceName shouldBe serviceName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Service Tag Engine through Yaml with default storage" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
storage:
  inMemory:
services:
- name: $serviceName
  tagEngine:
          """,
          )
        }
        with(worker.getServiceTagEngineConfig(serviceName)) {
          this?.serviceName shouldBe serviceName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow Executor through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .addWorkflowExecutor(workflowExecutor)
              .build()
        }
        worker.getWorkflowExecutorConfig(workflowName) shouldBe workflowExecutor
      }

      "Can create Infinitic Worker as Workflow Executor through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
workflows:
- name: $workflowName
  executor:
    class: $workflowClass
          """,
          )
        }
        with(worker.getWorkflowExecutorConfig(workflowName)) {
          this?.workflowName shouldBe workflowName
          this?.factories?.get(0)?.invoke().shouldBeInstanceOf<WorkflowAImpl>()
        }
      }

      "Can create Infinitic Worker as Workflow Tag Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .addWorkflowTagEngine(workflowTagEngine)
              .build()
        }
        worker.getWorkflowTagEngineConfig(workflowName) shouldBe workflowTagEngine
      }

      "Can create Infinitic Worker as Workflow Tag Engine through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
workflows:
- name: $workflowName
  tagEngine:
    storage:
      inMemory:
          """,
          )
        }
        with(worker.getWorkflowTagEngineConfig(workflowName)) {
          this?.workflowName shouldBe workflowName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow Tag Engine through Yaml with default storage" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
storage:
  inMemory:
workflows:
- name: $workflowName
  tagEngine:
          """,
          )
        }
        with(worker.getWorkflowTagEngineConfig(workflowName)) {
          this?.workflowName shouldBe workflowName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow State Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .addWorkflowStateEngine(workflowStateEngine)
              .build()
        }
        worker.getWorkflowStateEngineConfig(workflowName) shouldBe workflowStateEngine
      }

      "Can create Infinitic Worker as Workflow State Engine through Yaml" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
workflows:
- name: $workflowName
  stateEngine:
    storage:
      inMemory:
          """,
          )
        }
        with(worker.getWorkflowStateEngineConfig(workflowName)) {
          this?.workflowName shouldBe workflowName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow State Engine through Yaml with default storage" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
storage:
  inMemory:
workflows:
- name: $workflowName
  stateEngine:
          """,
          )
        }
        with(worker.getWorkflowStateEngineConfig(workflowName)) {
          this?.workflowName shouldBe workflowName
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create complete Infinitic Worker through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(transport)
              .setEventListener(eventListener)
              .addServiceExecutor(serviceExecutor)
              .addServiceTagEngine(serviceTagEngine)
              .addWorkflowTagEngine(workflowTagEngine)
              .addWorkflowExecutor(workflowExecutor)
              .addWorkflowStateEngine(workflowStateEngine)
              .build()
        }
        worker.getEventListenerConfig() shouldBe eventListener
        worker.getServiceExecutorConfig(serviceName) shouldBe serviceExecutor
        worker.getServiceTagEngineConfig(serviceName) shouldBe serviceTagEngine
        worker.getWorkflowTagEngineConfig(workflowName) shouldBe workflowTagEngine
        worker.getWorkflowExecutorConfig(workflowName) shouldBe workflowExecutor
        worker.getWorkflowStateEngineConfig(workflowName) shouldBe workflowStateEngine
      }

      "Can create complete Infinitic Worker through Yaml with default storage" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.fromYamlString(
              """
transport: inMemory
eventListener:
  class: ${TestEventListener::class.java.name}
storage:
  inMemory:
services:
- name: $serviceName
  executor:
    class: $serviceClass
  tagEngine:
workflows:
- name: $workflowName
  executor:
    class: $workflowClass
  stateEngine:
  tagEngine:
          """,
          )
        }
        worker.getEventListenerConfig() shouldNotBe null
        worker.getServiceExecutorConfig(serviceName) shouldNotBe null
        worker.getServiceTagEngineConfig(serviceName) shouldNotBe null
        worker.getWorkflowTagEngineConfig(workflowName) shouldNotBe null
        worker.getWorkflowExecutorConfig(workflowName) shouldNotBe null
        worker.getWorkflowStateEngineConfig(workflowName) shouldNotBe null
      }

      "explicit storage should not be replaced by default" {
        val db = MySQLConfig(host = "localhost", port = 3306, username = "root", password = "p")
        val config = InfiniticWorkerConfig.fromYamlString(
            """
transport: inMemory
storage:
  compression: bzip2
  mysql:
    host: ${db.host}
    port: ${db.port}
    username: ${db.username}
    password: ${db.password}

services:
  - name: $serviceName
    tagEngine:
      storage:
         inMemory:
workflows:
  - name: $workflowName
    stateEngine:
      storage:
         inMemory:
    tagEngine:
      storage:
         inMemory:
""",
        )
        with(config.services.first { it.name == serviceName }) {
          tagEngine?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
        with(config.workflows.first { it.name == workflowName }) {
          stateEngine?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
          tagEngine?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "startAsync() with Event Listener should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .setEventListener(eventListener)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "startAsync() with Service Executor should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceExecutor(serviceExecutor)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "startAsync() with Service Tag Engine should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceTagEngine(serviceTagEngine)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "startAsync() with Workflow Tag Engine should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowTagEngine(workflowTagEngine)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "startAsync() with Workflow Executor should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowExecutor(workflowExecutor)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "startAsync() with Workflow State Engine should not block" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowStateEngine(workflowStateEngine)
            .build()
        var flag = false
        later {
          flag = true
        }
        worker.startAsync()
        flag shouldBe false
        worker.close()
      }

      "start() with Event Listener should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .setEventListener(eventListener)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }

      "start() with Service Executor should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceExecutor(serviceExecutor)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }

      "start() with Service Tag Engine should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addServiceTagEngine(serviceTagEngine)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }

      "start() with Workflow Tag Engine should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowTagEngine(workflowTagEngine)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }

      "start() with Workflow Executor should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowExecutor(workflowExecutor)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }

      "start() with Workflow State Engine should block, and be released when closed" {
        val worker = InfiniticWorker.builder()
            .setTransport(transport)
            .addWorkflowStateEngine(workflowStateEngine)
            .build()

        var flag = false
        later(100) {
          flag = true
          worker.close()
        }
        worker.start()
        flag shouldBe true
      }
    },
)
