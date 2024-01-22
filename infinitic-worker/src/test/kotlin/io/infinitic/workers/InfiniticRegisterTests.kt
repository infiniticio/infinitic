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

import com.sksamuel.hoplite.ConfigException
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.samples.EventListenerImpl
import io.infinitic.workers.samples.ServiceEventListenerImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

val yaml = """
transport: inMemory
storage: inMemory
"""

internal class InfiniticRegisterTests :
  StringSpec(
      {
        "I can deploy a service  without tag engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    tagEngine: null
              """,
          )
          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val tagEngine = register.registry.getRegisteredServiceTag(serviceName)
          val service = register.registry.getRegisteredService(serviceName)

          service shouldNotBe null
          tagEngine shouldBe null
        }

        "I can deploy a service tag engin without service" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: io.infinitic.workers.samples.ServiceA
    tagEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val tagEngine = register.registry.getRegisteredServiceTag(serviceName)
          val service = register.registry.getRegisteredService(serviceName)

          service shouldBe null
          tagEngine shouldNotBe null
        }

        "I can deploy a workflow without tag engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: io.infinitic.workers.samples.WorkflowA
    class: io.infinitic.workers.samples.WorkflowAImpl
    tagEngine: null
              """,
          )
          val register = InfiniticRegister("log", config)
          val workflowName = WorkflowName("io.infinitic.workers.samples.WorkflowA")
          val tagEngine = register.registry.getRegisteredWorkflowTag(workflowName)
          val workflow = register.registry.getRegisteredWorkflow(workflowName)

          workflow shouldNotBe null
          tagEngine shouldBe null
        }

        "I can deploy a workflow tag engine without workflow" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: io.infinitic.workers.samples.WorkflowA
    tagEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegister("log", config)
          val workflowName = WorkflowName("io.infinitic.workers.samples.WorkflowA")
          val tagEngine = register.registry.getRegisteredWorkflowTag(workflowName)
          val workflow = register.registry.getRegisteredWorkflow(workflowName)

          workflow shouldBe null
          tagEngine shouldNotBe null
        }

        "I can deploy a workflow executor without workflow engine" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: io.infinitic.workers.samples.WorkflowA
    class: io.infinitic.workers.samples.WorkflowAImpl
    concurrency: 5
    workflowEngine: null
              """,
          )
          val register = InfiniticRegister("log", config)
          val workflowName = WorkflowName("io.infinitic.workers.samples.WorkflowA")
          val engine = register.registry.getRegisteredWorkflowEngine(workflowName)
          val workflow = register.registry.getRegisteredWorkflow(workflowName)

          workflow shouldNotBe null
          workflow!!.concurrency shouldBe 5
          engine shouldBe null
        }

        "I can deploy a workflow engine without workflow executor" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
workflows:
  - name: io.infinitic.workers.samples.WorkflowA
    workflowEngine:
      concurrency: 5
              """,
          )
          val register = InfiniticRegister("log", config)
          val workflowName = WorkflowName("io.infinitic.workers.samples.WorkflowA")
          val engine = register.registry.getRegisteredWorkflowEngine(workflowName)
          val workflow = register.registry.getRegisteredWorkflow(workflowName)

          workflow shouldBe null
          engine shouldNotBe null
          engine!!.concurrency shouldBe 5
        }


        "There is no default Service Event Listener" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
              """,
          )
          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)

          listener shouldBe null
        }

        "Service Event Listener can have its own value" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: io.infinitic.workers.samples.EventListenerImpl
  concurrency: 1
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
    eventListener:
      class: io.infinitic.workers.samples.ServiceEventListenerImpl
      concurrency: 100
              """,
          )

          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.eventListener.shouldBeInstanceOf<ServiceEventListenerImpl>()
          listener.concurrency shouldBe 100
        }

        "Service Event Listener concurrency is used when set" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
eventListener:
  class: io.infinitic.workers.samples.EventListenerImpl
  concurrency: 1
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
          )

          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.concurrency shouldBe 100
        }

        "Service Event Listener concurrency should inherit from Service" {
          val config = WorkerConfig.fromYaml(
              yaml,
              """
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
    eventListener:
      class: io.infinitic.workers.samples.EventListenerImpl
              """,
          )

          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
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
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 1
              """,
          )

          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
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
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
          )

          val register = InfiniticRegister("log", config)
          val serviceName = "io.infinitic.workers.samples.ServiceA".serviceName()
          val listener = register.registry.getRegisteredServiceEventListener(serviceName)!!

          listener.concurrency shouldBe 100
          listener.eventListener.shouldBeInstanceOf<EventListenerImpl>()
        }

        "if Event Listener class is not defined, it should throw an exception" {
          val e = shouldThrow<ConfigException> {
            WorkerConfig.fromYaml(
                yaml,
                """
services:
  - name: io.infinitic.workers.samples.ServiceA
    class: io.infinitic.workers.samples.ServiceAImpl
    concurrency: 10
    eventListener:
      concurrency: 100
              """,
            )
          }

          e.message.shouldContain("No `class` is defined for the event listener of service 'io.infinitic.workers.samples.ServiceA'")
        }
      },
  )


private fun String.serviceName() = ServiceName(this)
