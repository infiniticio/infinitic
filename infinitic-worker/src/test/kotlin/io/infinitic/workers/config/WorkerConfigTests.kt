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

import com.sksamuel.hoplite.ConfigException
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.workers.register.config.UNDEFINED_RETRY
import io.infinitic.workers.register.config.UNDEFINED_TIMEOUT
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowAImpl
import io.infinitic.workers.samples.WorkflowAImpl2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

internal class WorkerConfigTests :
  StringSpec(
      {

        val serviceName = ServiceName(ServiceA::class.java.name)
        val serviceImplName = ServiceAImpl::class.java.name

        "Unknown class in ignoredExceptions should throw" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>("""
transport: inMemory
serviceDefault:
    retry:
        ignoredExceptions:
            - foobar
""")
          }
          e.message!! shouldContain "Class 'foobar' not found"
        }

        "Unknown class in ignoredExceptions in task should throw" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>("""
transport: inMemory
services:
    - name: $serviceName
      class: $serviceImplName
      retry:
        ignoredExceptions:
          - foobar
""")
          }
          e.message!! shouldContain "Class 'foobar' not found"
        }

        "No Exception class in ignoredExceptions should throw" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>("""
transport: inMemory
serviceDefault:
  retry:
    ignoredExceptions:
      - io.infinitic.workers.InfiniticWorker
""")
          }
          e.message!! shouldContain
              "'io.infinitic.workers.InfiniticWorker' in ignoredExceptions must be an Exception"
        }

        "No Exception class in ignoredExceptions in task should throw" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>("""
transport: inMemory
services:
    - name: $serviceName
      class: $serviceImplName
      retry:
        ignoredExceptions:
          - io.infinitic.workers.InfiniticWorker
""")
          }
          e.message!! shouldContain
              "'io.infinitic.workers.InfiniticWorker' in ignoredExceptions must be an Exception"
        }

        "timeout in task should be positive" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>("""
transport: inMemory
services:
    - name: $serviceName
      class: $serviceImplName
      timeoutInSeconds: 0
""")
          }
          e.message!! shouldContain "timeoutInSeconds"
        }

        "task instance should not be reused" {
          val config = WorkerConfig.fromResource("/config/services/instance.yml")

          val task = config.services.first { it.name == ServiceA::class.java.name }
          task.getInstance() shouldNotBe task.getInstance()
        }

        "task with InvocationTargetException should throw cause" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/invocationTargetException.yml")
          }
          e.message!! shouldContain
              "Error during class 'io.infinitic.workers.samples.ServiceWithInvocationTargetException' instantiation"
        }

        "workflow with InvocationTargetException should throw cause" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/invocationTargetException.yml")
          }
          e.message!! shouldContain
              "Error during class 'io.infinitic.workers.samples.WorkflowWithInvocationTargetException' instantiation"
        }

        "task with ExceptionInInitializerError should throw cause" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/exceptionInInitializerError.yml")
          }
          e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
        }

        "workflow with ExceptionInInitializerError should throw cause" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/exceptionInInitializerError.yml")
          }
          e.message!! shouldContain "Underlying error was java.lang.ExceptionInInitializerError"
        }

        "service Unknown" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/unknown.yml")
          }
          e.message!! shouldContain "Class 'io.infinitic.workers.samples.UnknownService' not found"
        }

        "Service Event Listener Unknown" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/unknownListener.yml")
          }
          e.message!! shouldContain "Class 'io.infinitic.workers.samples.UnknownListener' not found"
        }

        "not a Service Event Listener" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/incompatibleListener.yml")
          }
          e.message!! shouldContain
              "Class 'io.infinitic.workers.samples.ServiceAImpl' must implement '${CloudEventListener::class.java.name}'"
        }

        "Service Event Listener should inherit concurrency from Service" {
          val config =
              WorkerConfig.fromResource("/config/services/instanceWithListener.yml")

          config.services
        }

        "workflow Unknown" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/unknown.yml")
          }
          e.message!! shouldContain "Class 'io.infinitic.workers.samples.UnknownWorkflow' not found"
        }

        "not a workflow" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/notAWorkflow.yml")
          }
          e.message!! shouldContain
              "Class 'io.infinitic.workers.samples.NotAWorkflow' must extend 'io.infinitic.workflows.Workflow'"
        }

        "checking default service config" {
          val config = WorkerConfig.fromResource("/config/services/instance.yml")

          config.serviceDefault shouldBe null
          config.services.size shouldBe 1
          config.services[0].retry shouldBe UNDEFINED_RETRY
          config.services[0].timeoutInSeconds shouldBe UNDEFINED_TIMEOUT
          config.services[0].concurrency shouldBe null
        }

        "checking default workflow config" {
          val config = WorkerConfig.fromResource("/config/workflows/instance.yml")

          config.workflowDefault shouldBe null
          config.workflows.size shouldBe 1
          config.workflows[0].retry shouldBe UNDEFINED_RETRY
          config.workflows[0].timeoutInSeconds shouldBe UNDEFINED_TIMEOUT
          config.workflows[0].checkMode shouldBe null
          config.workflows[0].concurrency shouldBe null
        }

        "checking the compatibility between name and class in services" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/services/incompatibleServiceName.yml")
          }
          e.message!! shouldContain
              "Class '${ServiceAImpl::class.java.name}' is not an implementation of this service"
        }

        "checking the compatibility between name and class in workflows" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/incompatibleWorkflowName.yml")
          }
          e.message!! shouldContain
              "Class '${WorkflowAImpl::class.java.name}' is not an implementation of 'UnknownWorkflow'"
        }

        "checking the compatibility between name and classes in workflows" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromResource("/config/workflows/incompatibleWorkflowsName.yml")
          }
          e.message!! shouldContain
              "Class '${WorkflowAImpl2::class.java.name}' is not an implementation of 'io.infinitic.workers.samples.WorkflowA'"
        }
      },
  )
