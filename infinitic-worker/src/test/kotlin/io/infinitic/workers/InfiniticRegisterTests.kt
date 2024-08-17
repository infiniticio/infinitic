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

import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.register.InfiniticRegisterImpl
import io.infinitic.workers.samples.EventListenerImpl
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.ServiceEventListenerImpl
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.infinitic.workflows.WorkflowCheckMode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.security.InvalidParameterException

private const val yaml = """
transport: inMemory
storage: inMemory
"""

internal class InfiniticRegisterTests :
  StringSpec(
      {
        val serviceName = ServiceName(ServiceA::class.java.name)
        val serviceImplName = ServiceAImpl::class.java.name
        val workflowName = WorkflowName(WorkflowA::class.java.name)
        val workflowImplName = WorkflowAImpl::class.java.name
        val eventListenerImpl = EventListenerImpl::class.java.name

        "checking default for service" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
    - name: $serviceName
      class: $serviceImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withTimeout shouldBe null
          service.withRetry shouldBe null
          service.concurrency shouldBe 1
        }

        "get timeout for service executor from default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  timeoutInSeconds: 1
services:
    - name:  $serviceName
      class: $serviceImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withTimeout?.getTimeoutInSeconds() shouldBe 1.0
        }

        "explicit timeout in service should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  timeoutInSeconds: 1
services:
    - name:  $serviceName
      class: $serviceImplName
      timeoutInSeconds: 2
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withTimeout?.getTimeoutInSeconds() shouldBe 2.0
        }

        "explicit null timeout in service should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  timeoutInSeconds: 1
services:
    - name: $serviceName
      class: $serviceImplName
      timeoutInSeconds: null
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withTimeout shouldBe null
        }

        "get retry for service executor from default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
transport: inMemory
serviceDefault:
  retry:
    maximumRetries: 42
services:
    - name: $serviceName
      class: $serviceImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withRetry!!.getSecondsBeforeRetry(42, Exception()) shouldBe null
          service.withRetry!!.getSecondsBeforeRetry(41, Exception()) shouldNotBe null
        }

        "explicit retry in service should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 42
services:
    - name:  $serviceName
      class: $serviceImplName
      retry:
        maximumRetries: 100
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withRetry!!.getSecondsBeforeRetry(100, Exception()) shouldBe null
          service.withRetry!!.getSecondsBeforeRetry(99, Exception()) shouldNotBe null
        }

        "explicit null retry in service should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 42
services:
    - name:  $serviceName
      class: $serviceImplName
      retry: null
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)!!
          service.withRetry shouldBe null
        }

        "checking default in workflow" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
    - name: $workflowName
      class: $workflowImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withTimeout shouldBe null
          workflow.withRetry shouldBe null
          workflow.concurrency shouldBe 1
          workflow.checkMode shouldBe null


        }

        "get timeout for workflow executor from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
workflowDefault:
  timeoutInSeconds: 1
workflows:
    - name: $workflowName
      class: $workflowImplName
""",
              )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withTimeout?.getTimeoutInSeconds() shouldBe 1.0
        }

        "explicit timeout in workflow should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  timeoutInSeconds: 1
workflows:
    - name: $workflowName
      class: $workflowImplName
      timeoutInSeconds: 2
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withTimeout?.getTimeoutInSeconds() shouldBe 2.0
        }

        "explicit null timeout in workflow should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  timeoutInSeconds: 1
workflows:
    - name: $workflowName
      class: $workflowImplName
      timeoutInSeconds: null
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withTimeout?.getTimeoutInSeconds() shouldBe null
        }

        "get retry for workflow executor from default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflowDefault:
  retry:
    maximumRetries: 42
workflows:
    - name: $workflowName
      class: $workflowImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withRetry!!.getSecondsBeforeRetry(42, Exception()) shouldBe null
          workflow.withRetry!!.getSecondsBeforeRetry(41, Exception()) shouldNotBe null
        }

        "explicit retry in workflow executor should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 42
workflows:
    - name: $workflowName
      class: $workflowImplName
      retry:
        maximumRetries: 100
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withRetry!!.getSecondsBeforeRetry(100, Exception()) shouldBe null
          workflow.withRetry!!.getSecondsBeforeRetry(99, Exception()) shouldNotBe null
        }

        "explicit null retry in workflow executor should not be superseded with default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 42
workflows:
    - name: $workflowName
      class: $workflowImplName
      retry: null
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.withRetry shouldBe null
        }

        "get workflow checkmode from default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflowDefault:
  checkMode: strict
workflows:
    - name: $workflowName
      class: $workflowImplName
""",
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val workflow = register.registry.getRegisteredWorkflowExecutor(workflowName)!!
          workflow.checkMode shouldBe WorkflowCheckMode.strict
        }

        "service executor do not retry if maximumRetries = 0" {
          val workerConfig = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 0
""",
          )
          workerConfig.serviceDefault?.retry shouldNotBe null
          workerConfig.serviceDefault?.retry!!.getSecondsBeforeRetry(0, Exception()) shouldBe null
        }

        "do not retry once reach maximumRetries" {
          val workerConfig = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    maximumRetries: 10
""",
          )
          workerConfig.serviceDefault?.retry shouldNotBe null
          workerConfig.serviceDefault?.retry!!.getSecondsBeforeRetry(
              9,
              Exception(),
          ) shouldNotBe null
          workerConfig.serviceDefault?.retry!!.getSecondsBeforeRetry(10, Exception()) shouldBe null
        }

        "do not retry for non retryable exception" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  retry:
    ignoredExceptions:
        - ${TestException::class.java.name}
""",
          )
          config.serviceDefault?.retry shouldNotBe null
          config.serviceDefault?.retry!!.getSecondsBeforeRetry(1, Exception()) shouldNotBe null
          config.serviceDefault?.retry!!.getSecondsBeforeRetry(
              1,
              TestException(),
          ) shouldBe null
          config.serviceDefault?.retry!!.getSecondsBeforeRetry(
              1,
              ChildTestException(),
          ) shouldBe null
        }

        "I can deploy a service  without tag engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: $serviceName
    class: $serviceImplName
    tagEngine: null
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val tagEngine = register.registry.getRegisteredServiceTagEngine(serviceName)
          val executor = register.registry.getRegisteredServiceExecutor(serviceName)

          executor shouldNotBe null
          tagEngine shouldBe null
        }

        "I can deploy a service tag engin without service" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: $serviceName
    tagEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val tagEngine = register.registry.getRegisteredServiceTagEngine(serviceName)
          val executor = register.registry.getRegisteredServiceExecutor(serviceName)

          executor shouldBe null
          tagEngine shouldNotBe null
        }

        "I can deploy a workflow without tag engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: $workflowName
    class: $workflowImplName
    tagEngine: null
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val tagEngine = register.registry.getRegisteredWorkflowTagEngine(workflowName)
          val executor = register.registry.getRegisteredWorkflowExecutor(workflowName)

          executor shouldNotBe null
          tagEngine shouldBe null
        }

        "I can deploy a workflow tag engine without workflow" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: $workflowName
    tagEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val tagEngine = register.registry.getRegisteredWorkflowTagEngine(workflowName)
          val executor = register.registry.getRegisteredWorkflowExecutor(workflowName)

          executor shouldBe null
          tagEngine shouldNotBe null
        }

        "I can deploy a workflow executor without workflow engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: $workflowName
    class: $workflowImplName
    concurrency: 5
    stateEngine: null
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val engine = register.registry.getRegisteredWorkflowStateEngine(workflowName)
          val executor = register.registry.getRegisteredWorkflowExecutor(workflowName)

          executor shouldNotBe null
          executor!!.concurrency shouldBe 5
          engine shouldBe null
        }

        "I can deploy a workflow engine without workflow executor" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: $workflowName
    stateEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val engine = register.registry.getRegisteredWorkflowStateEngine(workflowName)
          val executor = register.registry.getRegisteredWorkflowExecutor(workflowName)

          executor shouldBe null
          engine shouldNotBe null
          engine!!.concurrency shouldBe 5
        }


        "There is no default Service Event Listener" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: $serviceName
    class: $serviceImplName
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)

          listener shouldBe null
        }

        "Explicit Service Event Listener is not superseded by default" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: $eventListenerImpl
  concurrency: 1
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 10
    eventListener:
      class: ${ServiceEventListenerImpl::class.java.name}
      concurrency: 100
              """,
          )

          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.eventListener.shouldBeInstanceOf<ServiceEventListenerImpl>()
          listener.concurrency shouldBe 100
        }

        "Service Event Listener concurrency is used when set" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: $eventListenerImpl
  concurrency: 1
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
          )

          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.concurrency shouldBe 100
        }

        "Service Event Listener concurrency should inherit from Service" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 10
    eventListener:
      class: $eventListenerImpl
              """,
          )

          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.concurrency shouldBe 10
        }

        "Service Event Listener concurrency should inherit from default Event Listener if set" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: io.infinitic.workers.samples.EventListenerImpl
  concurrency: 10
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 1
              """,
          )

          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!
          listener.concurrency shouldBe 10
          listener.eventListener.shouldBeInstanceOf<EventListenerImpl>()
        }

        "if Service Event Listener concurrency is  defined, it should be used" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: io.infinitic.workers.samples.EventListenerImpl
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
          )

          val register = InfiniticRegisterImpl.fromConfig(config)
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.concurrency shouldBe 100
          listener.eventListener.shouldBeInstanceOf<EventListenerImpl>()
        }

        "if Event Listener class is not defined, it should throw an exception" {
          val e = shouldThrow<InvalidParameterException> {
            val config = WorkerConfig.fromYaml(
                yaml,
                """
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
            )
            InfiniticRegisterImpl.fromConfig(config)
          }

          e.message.shouldContain("Missing CloudEventListener at registration")
        }

        "serviceDefault is used if NOT present" {
          val concurrency = 10
          val timeoutInSeconds = 400.0
          val maxRetries = 2
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  concurrency: $concurrency
  timeoutInSeconds: $timeoutInSeconds
  retry:
    maximumRetries: $maxRetries
services:
  - name: $serviceName
    class: $serviceImplName
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)

          val service = register.registry.getRegisteredServiceExecutor(serviceName)

          service shouldNotBe null
          service?.concurrency shouldBe concurrency
          service?.withTimeout?.getTimeoutInSeconds() shouldBe timeoutInSeconds
          service?.withRetry?.getSecondsBeforeRetry(maxRetries, Exception()) shouldBe null
        }

        "serviceDefault is NOT used if explicit configuration is present" {
          val concurrency = 10
          val timeoutInSeconds = 400.0
          val maxRetries = 2
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  concurrency: $concurrency
  timeoutInSeconds: $timeoutInSeconds
  retry:
    maximumRetries: $maxRetries
services:
  - name: $serviceName
    class: $serviceImplName
    concurrency: 7
    timeoutInSeconds: 100
    retry:
       maximumRetries: 1
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          val service = register.registry.getRegisteredServiceExecutor(serviceName)

          service shouldNotBe null
          service?.concurrency shouldBe 7
          service?.withTimeout?.getTimeoutInSeconds() shouldBe 100.0
          service?.withRetry?.getSecondsBeforeRetry(1, Exception()) shouldBe null
        }

        "serviceDefault is used, with a manual registration" {
          val concurrency = 10
          val timeoutInSeconds = 400.0
          val maxRetries = 2
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  concurrency: $concurrency
  timeoutInSeconds: $timeoutInSeconds
  retry:
    maximumRetries: $maxRetries
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          register.registerServiceExecutor(serviceName.toString(), { })

          val service = register.registry.getRegisteredServiceExecutor(serviceName)

          service shouldNotBe null
          service?.concurrency shouldBe concurrency
          service?.withTimeout?.getTimeoutInSeconds() shouldBe timeoutInSeconds
          service?.withRetry?.getSecondsBeforeRetry(maxRetries, Exception()) shouldBe null
        }

        "serviceDefault is NOT used, with a manual registration where configuration is present" {
          val concurrency = 10
          val timeoutInSeconds = 400.0
          val maxRetries = 2
          val config = WorkerConfig.fromYaml(
              yaml,
              """
serviceDefault:
  concurrency: $concurrency
  timeoutInSeconds: $timeoutInSeconds
  retry:
    maximumRetries: $maxRetries
              """,
          )
          val register = InfiniticRegisterImpl.fromConfig(config)
          register.registerServiceExecutor(
              serviceName = serviceName.toString(),
              serviceFactory = { },
              concurrency = 7,
              withTimeout = { 100.0 },
              withRetry = { _: Int, _: Exception -> null },
          )

          val service = register.registry.getRegisteredServiceExecutor(serviceName)

          service shouldNotBe null
          service?.concurrency shouldBe 7
          service?.withTimeout?.getTimeoutInSeconds() shouldBe 100.0
          service?.withRetry?.getSecondsBeforeRetry(maxRetries, Exception()) shouldBe null
        }
      },
  )

private open class TestException : Exception()

private class ChildTestException : TestException()
