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
package io.infinitic.tests.errors

import io.infinitic.common.fixtures.later
import io.infinitic.exceptions.TaskFailedException
import io.infinitic.exceptions.WorkflowCanceledException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowUnknownException
import io.infinitic.tests.Test
import io.infinitic.tests.channels.ChannelsWorkflow
import io.infinitic.tests.utils.UtilService
import io.infinitic.tests.utils.UtilWorkflow
import io.infinitic.workflows.DeferredStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

internal class ErrorsWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val errorsWorkflow = client.newWorkflow(
            ErrorsWorkflow::class.java, tags = setOf("foo", "bar"),
        )
        val utilWorkflow = client.newWorkflow(UtilWorkflow::class.java)

        "Cancelling workflow" {
          val deferred = client.dispatch(errorsWorkflow::waiting)

          later {
            val w = client.getWorkflowById(ErrorsWorkflow::class.java, deferred.id)
            client.cancel(w)
          }

          shouldThrow<WorkflowCanceledException> { deferred.await() }
        }

        "try/catch a failing task" {
          errorsWorkflow.failing1() shouldBe "ko"
        }

        "failing task with Exception on main path should throw" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failingWithException() }

          val taskException = error.deferredException as TaskFailedException
          taskException.serviceName shouldBe UtilService::class.java.name
          taskException.workerException.name shouldBe Exception::class.java.name
        }

        // This test checks that a throwable triggering a message sent to DLQ is correctly handle by the engine
//        "failing task with Throwable on main path should throw" {
//          val error = shouldThrow<FailedWorkflowException> { errorsWorkflow.failingWithThrowable() }
//
//          val taskException = error.deferredException as FailedTaskException
//          taskException.serviceName shouldBe UtilService::class.java.name
//          taskException.workerException.name shouldBe Throwable::class.java.name
//          println(taskException.workerException)
//        }

        "failing async task on main path should not throw" {
          errorsWorkflow.failing2a() shouldBe 100
        }

        "failing task not on main path should not throw" {
          errorsWorkflow.failing3() shouldBe 100
        }

        "failing instruction not on main path should not throw" {
          errorsWorkflow.failing3b() shouldBe 100
        }

//            "Cancelling task on main path should throw " {
//                val error = shouldThrow<FailedWorkflowException> { workflowA.failing4() }
//
//                val cause = error.deferredException as CanceledTaskException
//                cause.taskName shouldBe TaskA::class.java.name
//            }
//
//            "Cancelling task not on main path should not throw " {
//                workflowA.failing5() shouldBe 100
//            }

        "Cancelling child workflow on main path should throw" {
          val error = shouldThrow<WorkflowFailedException> { utilWorkflow.cancelChild1() }
          val cause = error.deferredException as WorkflowCanceledException
          cause.workflowName shouldBe ChannelsWorkflow::class.java.name
        }

        "Cancelling child workflow not on main path should not throw" {
          utilWorkflow.cancelChild2() shouldBe 200L
        }

        "Failure in child workflow on main path should throw exception" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing6() }

          val cause1 = error.deferredException as WorkflowFailedException
          cause1.workflowName shouldBe ErrorsWorkflow::class.java.name

          val cause2 = cause1.deferredException as TaskFailedException
          cause2.serviceName shouldBe UtilService::class.java.name
        }

        "Failure in child workflow not on main path should not throw" {
          errorsWorkflow.failing7() shouldBe 100
        }

        "Failure in child workflow on main path should throw" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing7bis() }

          val cause1 = error.deferredException as WorkflowFailedException
          cause1.workflowName shouldBe ErrorsWorkflow::class.java.name
          cause1.workflowMethodName shouldBe "failingWithException"

          val cause2 = cause1.deferredException as TaskFailedException
          cause2.serviceName shouldBe UtilService::class.java.name
        }

        "Failure in child workflow on main path can be caught" {
          errorsWorkflow.failing7ter() shouldBe Exception::class.java.name
        }

        "Retry all failed tasks should restart a workflow" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing8() }

          val deferred = client.lastDeferred!!

          val cause = error.deferredException as TaskFailedException
          cause.serviceName shouldBe UtilService::class.java.name

          later {
            val w = client.getWorkflowById(ErrorsWorkflow::class.java, deferred.id)
            client.retryTasks(w, taskStatus = DeferredStatus.FAILED)
          }

          deferred.await() shouldBe "ok"
        }

        "Retry a failed task by id should restart a workflow" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing8() }
          val deferred = client.lastDeferred!!

          val cause = error.deferredException as TaskFailedException
          cause.serviceName shouldBe UtilService::class.java.name

          later {
            val w = client.getWorkflowById(ErrorsWorkflow::class.java, deferred.id)
            client.retryTasks(w, taskId = cause.taskId)
          }

          deferred.await() shouldBe "ok"
        }

        "Retry a failed task by class should restart a workflow" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing8() }
          val deferred = client.lastDeferred!!

          val cause = error.deferredException as TaskFailedException
          cause.serviceName shouldBe UtilService::class.java.name

          later {
            val w = client.getWorkflowById(ErrorsWorkflow::class.java, deferred.id)
            client.retryTasks(w, taskClass = UtilService::class.java)
          }

          deferred.await() shouldBe "ok"
        }

        "retry a caught failed task should not throw and task status can change after retry" {
          errorsWorkflow.failing9() shouldBe true
        }

        "properties should be correctly set after a failed deferred" {
          errorsWorkflow.failing10() shouldBe "ok"
        }

        "Synchronous call of unknown workflow should throw" {
          val error = shouldThrow<WorkflowFailedException> { errorsWorkflow.failing11() }

          val cause = error.deferredException as WorkflowUnknownException
          cause.workflowName shouldBe ErrorsWorkflow::class.java.name
          cause.workflowId shouldBe "unknown"
        }

        "Synchronous call of unknown workflow can be caught" {
          errorsWorkflow.failing12() shouldBe "caught"
        }

        "Child workflow is canceled when parent workflow is canceled - tag are also added and deleted" {
          val tag = "toto"
          val taggedWorkflow = client.newWorkflow(ErrorsWorkflow::class.java, tags = setOf(tag))

          client.dispatch(taggedWorkflow::failing13)

          delay(1000)
          val w = client.getWorkflowByTag(ErrorsWorkflow::class.java, tag)
          val size = client.getIds(w).size

          client.cancel(w)

          delay(1000)
          client.getIds(w).size shouldBe size - 2
        }

        "Synchronous method call on workflow targeted by tag should throw NotImplementedError" {
          client.dispatch(errorsWorkflow::waiting)

          val w = client.getWorkflowByTag(ErrorsWorkflow::class.java, "foo")

          shouldThrow<NotImplementedError> { w.waiting() }
          // clean up
          client.cancel(w)
        }

        "Asynchronous method call on workflow targeted by tag should not throw NotImplementedError" {
          client.dispatch(errorsWorkflow::waiting)

          val w = client.getWorkflowByTag(ErrorsWorkflow::class.java, "foo")

          client.dispatch(w::waiting)

          // clean up
          client.cancel(w)
        }
      },
  )
