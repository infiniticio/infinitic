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

import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.Transport
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
import io.kotest.matchers.types.shouldBeInstanceOf

internal class InfiniticWorkerTests : StringSpec(
    {
      val storage = InMemoryStorageConfig(InMemoryConfig())

      val serviceName = ServiceA::class.java.name
      val serviceClass = ServiceAImpl::class.java.name
      val serviceTagEngine = ServiceTagEngineConfig.builder().setStorage(storage).build()
      val serviceExecutor = ServiceExecutorConfig.builder().setFactory { ServiceAImpl() }.build()

      val workflowName = WorkflowA::class.java.name
      val workflowClass = WorkflowAImpl::class.java.name
      val workflowTagEngine = WorkflowTagEngineConfig.builder().setStorage(storage).build()
      val workflowExecutor = WorkflowExecutorConfig.builder().addFactory { WorkflowAImpl() }.build()
      val workflowStateEngine = WorkflowStateEngineConfig.builder().setStorage(storage).build()

      "Can create Infinitic Worker as Service Executor through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addServiceExecutor(serviceName, serviceExecutor)
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
        }
      }

      "Can create Infinitic Worker as Service Tag Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addServiceTagEngine(serviceName, serviceTagEngine)
              .build()
        }
        worker.getServiceTagEngineConfig(serviceName) shouldBe serviceTagEngine
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
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow Executor through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addWorkflowExecutor(workflowName, workflowExecutor)
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
          this?.factories?.get(0)?.invoke().shouldBeInstanceOf<WorkflowAImpl>()
        }
      }

      "Can create Infinitic Worker as Workflow Tag Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addWorkflowTagEngine(workflowName, workflowTagEngine)
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
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create Infinitic Worker as Workflow State Engine through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addWorkflowStateEngine(workflowName, workflowStateEngine)
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
          this?.concurrency shouldBe 1
          this?.storage.shouldBeInstanceOf<InMemoryStorageConfig>()
        }
      }

      "Can create complete Infinitic Worker through builder" {
        val worker = shouldNotThrowAny {
          InfiniticWorker.builder()
              .setTransport(Transport.inMemory)
              .addServiceExecutor(serviceName, serviceExecutor)
              .addServiceTagEngine(serviceName, serviceTagEngine)
              .addWorkflowTagEngine(workflowName, workflowTagEngine)
              .addWorkflowExecutor(workflowName, workflowExecutor)
              .addWorkflowStateEngine(workflowName, workflowStateEngine)
              .build()
        }
        worker.getServiceExecutorConfig(serviceName) shouldBe serviceExecutor
        worker.getServiceTagEngineConfig(serviceName) shouldBe serviceTagEngine
        worker.getWorkflowTagEngineConfig(workflowName) shouldBe workflowTagEngine
        worker.getWorkflowExecutorConfig(workflowName) shouldBe workflowExecutor
        worker.getWorkflowStateEngineConfig(workflowName) shouldBe workflowStateEngine
      }
    },
)
