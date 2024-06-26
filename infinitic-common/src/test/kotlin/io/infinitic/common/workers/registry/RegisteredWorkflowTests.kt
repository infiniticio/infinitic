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
@file:Suppress("ClassName")

package io.infinitic.common.workers.registry

import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.workflows.UnknownWorkflowVersionException
import io.infinitic.workflows.Workflow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@Suppress("unused")
class RegisteredWorkflowTests :
  StringSpec(
      {
        class MyWorkflow : Workflow()
        class MyWorkflow_0 : Workflow()
        class MyWorkflow_1 : Workflow()
        class MyWorkflow_2 : Workflow()

        "List of classes must not be empty" {
          val e = shouldThrow<IllegalArgumentException> {
            RegisteredWorkflowExecutor(WorkflowName("foo"), listOf(), 42, null, null, null)
          }
          e.message shouldContain "List of factory must not be empty for workflow foo"
        }

        "Exception when workflows have same version" {
          val e = shouldThrow<IllegalArgumentException> {
            RegisteredWorkflowExecutor(
                WorkflowName("foo"),
                listOf({ MyWorkflow() }, { MyWorkflow_0() }),
                42,
                null,
                null,
                null,
            )
          }
          e.message shouldContain "have same version"
        }

        "Get instance with single class" {
          val rw = RegisteredWorkflowExecutor(
              WorkflowName("foo"), listOf { MyWorkflow() }, 42, null, null, null,
          )
          // get explicit version 0
          rw.getInstanceByVersion(WorkflowVersion(0))::class.java shouldBe MyWorkflow::class.java
          // get default version
          rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow::class.java
          // get unknown version
          val e =
              shouldThrow<UnknownWorkflowVersionException> {
                rw.getInstanceByVersion(
                    WorkflowVersion(
                        1,
                    ),
                )
              }
          e.message shouldContain "Unknown version \"1\""
        }

        "Get instance with single class with version" {
          val rw =
              RegisteredWorkflowExecutor(
                  WorkflowName("foo"), listOf { MyWorkflow_2() }, 42, null, null, null,
              )
          // get explicit version 0
          rw.getInstanceByVersion(WorkflowVersion(2))::class.java shouldBe MyWorkflow_2::class.java
          // get default version
          rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow_2::class.java
          // get unknown version
          val e =
              shouldThrow<UnknownWorkflowVersionException> {
                rw.getInstanceByVersion(
                    WorkflowVersion(
                        1,
                    ),
                )
              }
          e.message shouldContain "Unknown version \"1\""
        }

        "Get instance with multiple classes" {
          val rw =
              RegisteredWorkflowExecutor(
                  WorkflowName("foo"),
                  listOf({ MyWorkflow() }, { MyWorkflow_2() }),
                  42,
                  null,
                  null,
                  null,
              )
          // get explicit version 0
          rw.getInstanceByVersion(WorkflowVersion(0))::class.java shouldBe MyWorkflow::class.java
          // get explicit version 2
          rw.getInstanceByVersion(WorkflowVersion(2))::class.java shouldBe MyWorkflow_2::class.java
          // get default version
          rw.getInstanceByVersion(null)::class.java shouldBe MyWorkflow_2::class.java
          // get unknown version
          val e =
              shouldThrow<UnknownWorkflowVersionException> {
                rw.getInstanceByVersion(
                    WorkflowVersion(
                        1,
                    ),
                )
              }
          e.message shouldContain "Unknown version \"1\""
        }
      },
  )
