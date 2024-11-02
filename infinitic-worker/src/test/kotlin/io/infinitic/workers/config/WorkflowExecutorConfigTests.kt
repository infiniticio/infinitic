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
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.workers.config.WithExponentialBackoffRetry
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.samples.NotAWorkflow
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.WorkflowAImpl
import io.infinitic.workers.samples.WorkflowWithExceptionInInitializerError
import io.infinitic.workers.samples.WorkflowWithInvocationTargetException
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class WorkflowExecutorConfigTests : StringSpec(
    {
      val workflowName = WorkflowAImpl::class.java.name

      "Can create WorkflowExecutorConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(workflowName)
              .addFactory { WorkflowAImpl() }
              .build()
        }

        config.workflowName shouldBe workflowName
        config.shouldBeInstanceOf<WorkflowExecutorConfig>()
        config.factories.size shouldBe 1
        config.factories[0].invoke().shouldBeInstanceOf<WorkflowAImpl>()
        config.concurrency shouldBe 1
        config.withRetry shouldBe WithRetry.UNSET
        config.withTimeout shouldBe WithTimeout.UNSET
      }

      "Can create WorkflowExecutorConfig through builder with all parameters" {
        val withRetry = WithExponentialBackoffRetry()
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(workflowName)
              .addFactory { WorkflowAImpl() }
              .setConcurrency(10)
              .setTimeoutSeconds(3.0)
              .withRetry(withRetry)
              .setCheckMode(WorkflowCheckMode.strict)
              .build()
        }

        config.shouldBeInstanceOf<WorkflowExecutorConfig>()
        config.concurrency shouldBe 10
        config.withRetry shouldBe withRetry
        config.withTimeout?.getTimeoutSeconds() shouldBe 3.0
        config.checkMode shouldBe WorkflowCheckMode.strict
      }

      "workflowName is mandatory when building WorkflowExecutorConfig through builder" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowExecutorConfig.builder()
              .addFactory { WorkflowAImpl() }
              .build()
        }
        e.message shouldContain "workflowName"
      }

      "Concurrency must be positive when building WorkflowExecutorConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(workflowName)
              .addFactory { WorkflowAImpl() }
              .setConcurrency(0)
              .build()
        }
        e.message shouldContain "concurrency"
      }

      "Factory is mandatory when building WorkflowExecutorConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(workflowName)
              .setConcurrency(1)
              .build()
        }
        e.message shouldContain "factory"
      }

      "Can create WorkflowExecutorConfig through YAML without workflowName" {
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowAImpl::class.java.name}
          """,
          )
        }

        config.workflowName.isBlank() shouldBe true
        config.factories.size shouldBe 1
        config.factories[0].invoke().shouldBeInstanceOf<WorkflowAImpl>()
        config.concurrency shouldBe 1
        config.withRetry shouldBe WithRetry.UNSET
        config.withTimeout shouldBe WithTimeout.UNSET
      }

      "Can create WorkflowExecutorConfig through YAML with all parameters" {
        val withRetry = WithExponentialBackoffRetry(minimumSeconds = 4.0)
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowAImpl::class.java.name}
concurrency: 10
timeoutSeconds: 3.0
checkMode: strict
batch:
  maxMessages: 100
  maxSeconds: 0.5
retry:
  minimumSeconds: 4
          """,
          )
        }

        config.concurrency shouldBe 10
        config.withTimeout?.getTimeoutSeconds() shouldBe 3.0
        config.withRetry shouldBe withRetry
        config.checkMode shouldBe WorkflowCheckMode.strict
        config.batchConfig shouldBe BatchConfig(100, 0.5)
      }

      "class is mandatory when building WorkflowExecutorConfig from YAML" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
concurrency: 10
          """,
          )
        }

        e.message shouldContain "class"
      }

      "Unknown class in ignoredExceptions should throw" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowAImpl::class.java.name}
retry:
  ignoredExceptions:
    - foobar
""",
          )
        }
        e.message shouldContain "Class 'foobar' not found"
      }

      "Class not an Exception in ignoredExceptions should throw" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowAImpl::class.java.name}
retry:
  ignoredExceptions:
    - ${ServiceA::class.java.name}
""",
          )
        }
        e.message shouldContain "must be an Exception"
      }

      "timeout must be > 0 when building ServiceExecutorConfig from YAML" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowAImpl::class.java.name}
timeoutSeconds: 0
""",
          )
        }
        e.message shouldContain "timeoutSeconds must be > 0"
      }

      "InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowWithInvocationTargetException::class.java.name}
          """,
          )
        }
        e.message shouldContain
            "Error during class '${WorkflowWithInvocationTargetException::class.java.name}' instantiation"
      }

      "ExceptionInInitializer should throw cause" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${WorkflowWithExceptionInInitializerError::class.java.name}
          """,
          )
        }
        e.message shouldContain "ExceptionInInitializerError"
      }

      "workflow Unknown" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: io.infinitic.workers.samples.UnknownWorkflow
          """,
          )
        }
        e.message shouldContain "Class 'io.infinitic.workers.samples.UnknownWorkflow' not found"
      }

      "Not a workflow" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
class: ${NotAWorkflow::class.java.name}
          """,
          )
        }
        e.message shouldContain "Class '${NotAWorkflow::class.java.name}' must extend '${Workflow::class.java.name}'"
      }

      class MyWorkflow : Workflow()
      class MyWorkflow_0 : Workflow()
      class MyWorkflow_1 : Workflow()
      class MyWorkflow_2 : Workflow()
      class MyWorkflow_WithContext : Workflow() {
        val id = workflowId
      }

      "Can create WorkflowExecutorConfig with multiple versions" {
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(MyWorkflow::class.java.name)
              .addFactory { MyWorkflow() }
              .addFactory { MyWorkflow_1() }
              .addFactory { MyWorkflow_2() }
              .build()
        }

        config.shouldBeInstanceOf<WorkflowExecutorConfig>()
        config.factories.size shouldBe 3
        config.factories[0].invoke().shouldBeInstanceOf<MyWorkflow>()
        config.factories[1].invoke().shouldBeInstanceOf<MyWorkflow_1>()
        config.factories[2].invoke().shouldBeInstanceOf<MyWorkflow_2>()
      }

      "Can create WorkflowExecutorConfig through YAML with multiple versions" {
        val config = shouldNotThrowAny {
          WorkflowExecutorConfig.fromYamlString(
              """
classes:
  - ${MyWorkflow::class.java.name}
  - ${MyWorkflow_1::class.java.name}
  - ${MyWorkflow_2::class.java.name}
          """,
          )
        }
        config.shouldBeInstanceOf<WorkflowExecutorConfig>()
        config.factories.size shouldBe 3
        config.factories[0].invoke().shouldBeInstanceOf<MyWorkflow>()
        config.factories[1].invoke().shouldBeInstanceOf<MyWorkflow_1>()
        config.factories[2].invoke().shouldBeInstanceOf<MyWorkflow_2>()
      }

      "Exception when workflows have same version (through builder)" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(MyWorkflow::class.java.name)
              .addFactory { MyWorkflow() }
              .addFactory { MyWorkflow_0() }
              .addFactory { MyWorkflow_1() }
              .addFactory { MyWorkflow_2() }
              .build()
        }
        e.message shouldContain "duplicated"
      }

      "Exception when workflows have same version (through YAML)" {
        val e = shouldThrow<ConfigException> {
          WorkflowExecutorConfig.fromYamlString(
              """
classes:
  - ${MyWorkflow::class.java.name}
  - ${MyWorkflow_0::class.java.name}
  - ${MyWorkflow_1::class.java.name}
  - ${MyWorkflow_2::class.java.name}
          """,
          )
        }
        e.message shouldContain "duplicated"
      }

      "should NOT throw when factory returns the same instance but has no properties" {
        val w = MyWorkflow()
        shouldNotThrowAny {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(MyWorkflow::class.java.name)
              .addFactory { w }
              .build()
        }
      }

      "should throw when factory returns the same instance but has properties" {
        Workflow.setContext(emptyWorkflowContext)
        val w = MyWorkflow_WithContext()
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowExecutorConfig.builder()
              .setWorkflowName(MyWorkflow::class.java.name)
              .addFactory { w }
              .build()
        }
        e.message shouldContain "same object instance twice"
      }

//      "Exception when requesting unknown version" {
//        val e = shouldThrow<UnknownWorkflowVersionException> {
//          val w = MyWorkflow()
//          val r = RegisteredWorkflowExecutor(
//              WorkflowName("foo"),
//              listOf { w },
//              42,
//              null,
//              null,
//              null,
//          )
//          r.getInstanceByVersion(WorkflowVersion(1))
//        }
//        e.message shouldContain "Unknown version '1' for Workflow 'foo'"
//      }
//
//
//
//      "Get instance with single class" {
//        val rw = RegisteredWorkflowExecutor(
//            WorkflowName("foo"), listOf { MyWorkflow() }, 42, null, null, null,
//        )
//        // get explicit version 0
//        rw.getInstanceByVersion(WorkflowVersion(0))::class.java shouldBe MyWorkflow::class.java
//        // get default version
//        rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow::class.java
//        // get unknown version
//        val e = shouldThrow<UnknownWorkflowVersionException> {
//          rw.getInstanceByVersion(WorkflowVersion(1))
//        }
//        e.message shouldContain "Unknown version '1'"
//      }
//
//      "Get instance with single class with version" {
//        val rw =
//            RegisteredWorkflowExecutor(
//                WorkflowName("foo"), listOf { MyWorkflow_2() }, 42, null, null, null,
//            )
//        // get explicit version 0
//        rw.getInstanceByVersion(WorkflowVersion(2))::class.java shouldBe MyWorkflow_2::class.java
//        // get default version
//        rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow_2::class.java
//        // get unknown version
//        val e = shouldThrow<UnknownWorkflowVersionException> {
//          rw.getInstanceByVersion(WorkflowVersion(1))
//        }
//        e.message shouldContain "Unknown version '1'"
//      }
//
//      "Get instance with multiple classes" {
//        val rw = RegisteredWorkflowExecutor(
//            WorkflowName("foo"),
//            listOf({ MyWorkflow() }, { MyWorkflow_2() }),
//            42,
//            null,
//            null,
//            null,
//        )
//        // get explicit version 0
//        rw.getInstanceByVersion(WorkflowVersion(0))::class.java shouldBe MyWorkflow::class.java
//        // get explicit version 2
//        rw.getInstanceByVersion(WorkflowVersion(2))::class.java shouldBe MyWorkflow_2::class.java
//        // get default version
//        rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow_2::class.java
//        // get unknown version
//        val e = shouldThrow<UnknownWorkflowVersionException> {
//          rw.getInstanceByVersion(
//              WorkflowVersion(
//                  1,
//              ),
//          )
//        }
//        e.message shouldContain "Unknown version '1'"
//      }
//
//      "Get instance with single class using context" {
//        shouldNotThrowAny {
//          RegisteredWorkflowExecutor(
//              WorkflowName("foo"), listOf { MyWorkflow_WithContext() }, 42, null, null, null,
//          )
//        }
//      }
    },
)
