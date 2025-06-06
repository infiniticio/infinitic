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
package io.infinitic.tests.timeouts

import io.infinitic.Test
import io.infinitic.exceptions.TaskFailedException
import io.infinitic.exceptions.TaskTimedOutException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.utils.UtilService
import io.infinitic.utils.UtilServiceImpl
import io.infinitic.workflows.DeferredStatus
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

internal class TimeoutsWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val timeoutsWorkflow = client.newWorkflow(TimeoutsWorkflow::class.java)
        val iTimeoutsWorkflow = client.newWorkflow(ITimeoutWorkflow::class.java)

        "Synchronous call of a child-workflow running for more than its timeout should throw".config(
            timeout = 30.seconds,
        ) {
          val e = shouldThrow<WorkflowFailedException> { timeoutsWorkflow.withTimeoutOnChild(2000) }

          e.deferredException.shouldBeInstanceOf<WorkflowTimedOutException>()
          val cause = e.deferredException as WorkflowTimedOutException
          cause.workflowName shouldBe TimeoutsWorkflow::class.java.name
          cause.workflowMethodName shouldBe "withTimeoutOnMethod"
        }

        "Synchronous call of a workflow running for more than its timeout should throw" {
          shouldThrow<WorkflowTimedOutException> { timeoutsWorkflow.withTimeoutOnMethod(3000) }
        }

        "Synchronous call of a workflow running for less than its timeout should NOT throw" {
          shouldNotThrowAny { timeoutsWorkflow.withTimeoutOnMethod(10) shouldBe 10 }
        }

        "Synchronous call of a child-workflow running for less than its timeout should NOT throw" {
          shouldNotThrowAny { timeoutsWorkflow.withTimeoutOnChild(10) shouldBe 10 }
        }

        "timeout triggered on a synchronous child can be caught" {
          timeoutsWorkflow.withCaughtTimeoutOnChild(2000) shouldBe -1

          timeoutsWorkflow.withCaughtTimeoutOnTask(10) shouldBe 10
        }

        "Workflow with Timed out child will continue if child completes after the timeout" {
          shouldThrow<WorkflowFailedException> { timeoutsWorkflow.withTimeoutOnChild(2000) }
          // after the timeout, the workflow completes successfully
          client.lastDeferred!!.await() shouldBe 2000
        }

        "timeout triggered in a synchronous task should throw" {
          val e = shouldThrow<WorkflowFailedException> {
            timeoutsWorkflow.withTimeoutOnTask(2000)
          }

          e.deferredException.shouldBeInstanceOf<TaskTimedOutException>()
          val cause = e.deferredException as TaskTimedOutException
          cause.serviceName shouldBe UtilService::class.java.name
          cause.methodName shouldBe "withServiceTimeout"
        }

        "timeout on a synchronous task should NOT throw if slower than the task" {
          shouldNotThrowAny { timeoutsWorkflow.withTimeoutOnTask(10) }
        }

        " timeout triggered in a synchronous task should be manageable" {
          val e = shouldThrow<WorkflowFailedException> {
            timeoutsWorkflow.withManagedTimeoutOnTaskExecution()
          }

          e.deferredException.shouldBeInstanceOf<TaskFailedException>()

          UtilServiceImpl.onTimeout.get() shouldBe true
          UtilServiceImpl.hasTimedOut.get() shouldBe true
        }

        "Execution timeout triggered in a synchronous task should throw" {
          val e = shouldThrow<WorkflowFailedException> {
            timeoutsWorkflow.withTimeoutOnTaskExecution(2000)
          }

          e.deferredException.shouldBeInstanceOf<TaskFailedException>()
          val cause = e.deferredException as TaskFailedException
          cause.serviceName shouldBe UtilService::class.java.name
          cause.methodName shouldBe "withExecutionTimeout"
        }

        "Execution timeout on a synchronous task should NOT throw if slower than the task" {
          shouldNotThrowAny { timeoutsWorkflow.withTimeoutOnTaskExecution(10) }
        }

        "timeout triggered on a synchronous task can be caught" {
          timeoutsWorkflow.withCaughtTimeoutOnTask(2000) shouldBe -1

          timeoutsWorkflow.withCaughtTimeoutOnTask(10) shouldBe 10
        }

        "Workflow with Timed out task can be retried manually" {
          shouldThrow<WorkflowFailedException> { timeoutsWorkflow.withManualRetry() }
          // after the timeout, the workflow completes successfully
          val id = client.lastDeferred!!.id
          val w = client.getWorkflowById(TimeoutsWorkflow::class.java, id)
          client.retryTasks(w, taskStatus = DeferredStatus.TIMED_OUT)

          client.lastDeferred!!.await() shouldBe 1
        }

        "Timeout defined with WithTimeout interface should throw if timeout is reached" {
          shouldThrow<WorkflowTimedOutException> { iTimeoutsWorkflow.withTimeoutOnMethod(1000) }
        }

        "Timeout defined with WithTimeout interface should not throw if timeout is not reached" {
          shouldNotThrowAny { iTimeoutsWorkflow.withTimeoutOnMethod(10) shouldBe 10 }
        }
      },
  )
